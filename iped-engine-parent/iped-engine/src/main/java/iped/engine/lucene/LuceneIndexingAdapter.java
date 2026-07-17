package iped.engine.lucene;

import iped.index.spi.IndexedDocument;
import iped.index.spi.IndexingPort;
import iped.index.spi.IndexingSession;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;

/**
 * Reference {@link IndexingPort} adapter wrapping a single Worker's {@link IndexWriter}.
 *
 * <p><b>Instance-per-Worker, never a singleton.</b> Each instance wraps exactly the {@code
 * IndexWriter} of the Worker that created it, mirroring the Worker instance-per-thread model
 * ({@code iped.engine.core.Worker}). Sharing a single instance across Workers would read/write
 * against the wrong writer.
 *
 * <p>Not thread-safe beyond what the underlying {@code IndexWriter} already guarantees for
 * concurrent NRT reader opens; instances are not meant to be shared across threads.
 */
public final class LuceneIndexingAdapter implements IndexingPort {

  private final IndexWriter writer;

  public LuceneIndexingAdapter(IndexWriter writer) {
    this.writer = writer;
  }

  /**
   * Opens a near-real-time reader over {@link #writer} (applying deletes, see {@code
   * DirectoryReader.open(writer, true, true)}), iterates the sorted doc values of {@code field},
   * and collects distinct, non-empty values. Relocated verbatim from {@code DuplicateTask}'s
   * previous {@code init()} logic.
   */
  @Override
  public Stream<String> distinctFieldValues(String field) throws IOException {
    List<String> values = new ArrayList<>();
    withSession(session -> session.distinctFieldValues(field).forEach(values::add));
    return values.stream();
  }

  /**
   * Opens a near-real-time reader over {@link #writer} (same NRT-read semantics as {@link
   * #distinctFieldValues}) and invokes {@code consumer} once per currently-visible document with an
   * {@link IndexedDocument} view whose {@code getString}/{@code getNumeric} accessors lazily
   * resolve sorted doc-values, falling back to stored fields for {@code getString}, mirroring
   * {@link DocValuesUtil} advance semantics. The view (and the field-value caches it shares with
   * sibling views of the same scan) is only valid until the reader is closed at the end of this
   * method.
   *
   * <p>Delegates to {@link #withSession} so single-call and multi-call (session-based) scans share
   * the exact same reader-open/document-scan code path.
   */
  @Override
  public void forEachDocument(Consumer<IndexedDocument> consumer) throws IOException {
    withSession(session -> session.forEachDocument(consumer));
  }

  /**
   * Opens ONE near-real-time reader over {@link #writer}, hands a session bound to it to {@code
   * sessionConsumer}, then closes the reader. Fixes a performance regression where a caller issuing
   * several sequential {@link #forEachDocument}/{@link #distinctFieldValues} calls (e.g. {@code
   * SkipCommitedTask.init()}) paid for one NRT reader open per call -- NRT opens can trigger writer
   * flush/segment-merge overhead -- instead of one open for the whole unit of work.
   */
  @Override
  public void withSession(SessionConsumer sessionConsumer) throws IOException {
    try (IndexReader reader = DirectoryReader.open(writer, true, true)) {
      LeafReader aReader = SlowCompositeReaderWrapper.wrap(reader);
      sessionConsumer.accept(new LuceneIndexingSession(aReader));
    } catch (IndexNotFoundException e) {
      // index does not yet exist: session sees zero documents/values, exactly as
      // the previous per-call forEachDocument/distinctFieldValues behaved.
      sessionConsumer.accept(EMPTY_SESSION);
    }
  }

  private static final IndexingSession EMPTY_SESSION =
      new IndexingSession() {
        @Override
        public Stream<String> distinctFieldValues(String field) {
          return Stream.empty();
        }

        @Override
        public void forEachDocument(Consumer<IndexedDocument> consumer) {
          // no visible documents
        }

        @Override
        public boolean fieldExists(String field) {
          return false;
        }
      };

  /**
   * {@link IndexingSession} bound to a single already-open {@link LeafReader}, reused across every
   * field query/scan issued within one {@link #withSession} callback. Only the {@code LeafReader}
   * and the stateless {@code fieldExistsCache} are shared across multiple {@link #forEachDocument}
   * calls made against this session; the doc-values caches ({@code sortedCache}/{@code
   * numericCache}) are created fresh for each {@link #forEachDocument} call, since {@code
   * SortedDocValues}/{@code NumericDocValues} are forward-only iterators that cannot be reused
   * across a second full document scan.
   */
  private static final class LuceneIndexingSession implements IndexingSession {

    private final LeafReader aReader;
    private final Map<String, Boolean> fieldExistsCache = new HashMap<>();

    LuceneIndexingSession(LeafReader aReader) {
      this.aReader = aReader;
    }

    @Override
    public Stream<String> distinctFieldValues(String field) throws IOException {
      List<String> values = new ArrayList<>();
      SortedDocValues sdv = aReader.getSortedDocValues(field);
      if (sdv != null) {
        for (int ord = 0; ord < sdv.getValueCount(); ord++) {
          String value = sdv.lookupOrd(ord).utf8ToString();
          if (value != null && !value.isEmpty()) {
            values.add(value);
          }
        }
      }
      return values.stream();
    }

    @Override
    public void forEachDocument(Consumer<IndexedDocument> consumer) throws IOException {
      // sortedCache/numericCache MUST be scoped to this single forEachDocument
      // call, never shared across calls: SortedDocValues/NumericDocValues are
      // forward-only iterators (advanceExact only moves forward), so reusing the
      // same instance across a second full scan within the same session would
      // silently return null/false for every document once the first scan has
      // advanced past it, for sparse fields (not present on every document).
      // Only the LeafReader itself (and the stateless fieldExistsCache) are safe
      // to share across multiple forEachDocument calls in one session.
      Map<String, SortedDocValues> sortedCache = new HashMap<>();
      Map<String, NumericDocValues> numericCache = new HashMap<>();
      int maxDoc = aReader.maxDoc();
      for (int doc = 0; doc < maxDoc; doc++) {
        consumer.accept(
            new LuceneIndexedDocument(aReader, doc, sortedCache, numericCache, fieldExistsCache));
      }
    }

    @Override
    public boolean fieldExists(String field) {
      return fieldExistsCache.computeIfAbsent(
          field, f -> aReader.getFieldInfos().fieldInfo(f) != null);
    }
  }

  /**
   * {@link IndexedDocument} view backed by a single doc id of an open {@link LeafReader}. Caches
   * the per-field doc-values objects (not per-doc values) across a single {@code forEachDocument}
   * scan, mirroring how {@code SkipCommitedTask}'s previous direct-Lucene code fetched each
   * doc-values field once and advanced it per document. These doc-values caches are scoped to one
   * scan only -- see {@link LuceneIndexingSession#forEachDocument} -- since {@code
   * SortedDocValues}/{@code NumericDocValues} are forward-only.
   */
  private static final class LuceneIndexedDocument implements IndexedDocument {

    private final LeafReader reader;
    private final int doc;
    private final Map<String, SortedDocValues> sortedCache;
    private final Map<String, NumericDocValues> numericCache;
    private final Map<String, Boolean> fieldExistsCache;

    LuceneIndexedDocument(
        LeafReader reader,
        int doc,
        Map<String, SortedDocValues> sortedCache,
        Map<String, NumericDocValues> numericCache,
        Map<String, Boolean> fieldExistsCache) {
      this.reader = reader;
      this.doc = doc;
      this.sortedCache = sortedCache;
      this.numericCache = numericCache;
      this.fieldExistsCache = fieldExistsCache;
    }

    @Override
    public String getString(String field) {
      SortedDocValues sdv = sortedDocValues(field);
      if (sdv != null) {
        String value = DocValuesUtil.getVal(sdv, doc);
        if (value != null) {
          return value;
        }
      }
      // Fast path: if the field is completely absent from the index schema
      // (e.g. no item has produced it yet, such as NUM_SUBITEMS early in
      // processing), skip the expensive per-document stored-fields
      // deserialization entirely. Checked once per field name per scan
      // (mirrors the old SkipCommitedTask's one-time `sdv == null` check
      // outside its per-document loop), not once per document.
      if (!fieldExists(field)) {
        return null;
      }
      try {
        return reader.storedFields().document(doc).get(field);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    private boolean fieldExists(String field) {
      return fieldExistsCache.computeIfAbsent(
          field, f -> reader.getFieldInfos().fieldInfo(f) != null);
    }

    @Override
    public Long getNumeric(String field) {
      NumericDocValues ndv = numericDocValues(field);
      return ndv != null ? DocValuesUtil.get(ndv, doc) : null;
    }

    private SortedDocValues sortedDocValues(String field) {
      if (!sortedCache.containsKey(field)) {
        try {
          sortedCache.put(field, reader.getSortedDocValues(field));
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      return sortedCache.get(field);
    }

    private NumericDocValues numericDocValues(String field) {
      if (!numericCache.containsKey(field)) {
        try {
          numericCache.put(field, reader.getNumericDocValues(field));
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      return numericCache.get(field);
    }
  }
}
