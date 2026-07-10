package iped.engine.lucene;

import iped.index.spi.IndexedDocument;
import iped.index.spi.IndexingPort;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Reference {@link IndexingPort} adapter wrapping a single Worker's
 * {@link IndexWriter}.
 *
 * <p><b>Instance-per-Worker, never a singleton.</b> Each instance wraps
 * exactly the {@code IndexWriter} of the Worker that created it, mirroring
 * the Worker instance-per-thread model ({@code iped.engine.core.Worker}).
 * Sharing a single instance across Workers would read/write against the
 * wrong writer.
 *
 * <p>Not thread-safe beyond what the underlying {@code IndexWriter} already
 * guarantees for concurrent NRT reader opens; instances are not meant to be
 * shared across threads.
 */
public final class LuceneIndexingAdapter implements IndexingPort {

    private final IndexWriter writer;

    public LuceneIndexingAdapter(IndexWriter writer) {
        this.writer = writer;
    }

    /**
     * Opens a near-real-time reader over {@link #writer} (applying deletes,
     * see {@code DirectoryReader.open(writer, true, true)}), iterates the
     * sorted doc values of {@code field}, and collects distinct, non-empty
     * values. Relocated verbatim from {@code DuplicateTask}'s previous
     * {@code init()} logic.
     */
    @Override
    public Stream<String> distinctFieldValues(String field) throws IOException {
        List<String> values = new ArrayList<>();
        try (IndexReader reader = DirectoryReader.open(writer, true, true)) {
            LeafReader aReader = SlowCompositeReaderWrapper.wrap(reader);
            SortedDocValues sdv = aReader.getSortedDocValues(field);
            if (sdv != null) {
                for (int ord = 0; ord < sdv.getValueCount(); ord++) {
                    String value = sdv.lookupOrd(ord).utf8ToString();
                    if (value != null && !value.isEmpty()) {
                        values.add(value);
                    }
                }
            }
        } catch (IndexNotFoundException e) {
            // index does not yet exist: return an empty stream
            return Stream.empty();
        }
        return values.stream();
    }

    /**
     * Opens a near-real-time reader over {@link #writer} (same NRT-read
     * semantics as {@link #distinctFieldValues}) and invokes {@code consumer}
     * once per currently-visible document with an {@link IndexedDocument}
     * view whose {@code getString}/{@code getNumeric} accessors lazily
     * resolve sorted doc-values, falling back to stored fields for
     * {@code getString}, mirroring {@link DocValuesUtil} advance semantics.
     * The view (and the field-value caches it shares with sibling views of
     * the same scan) is only valid until the reader is closed at the end of
     * this method.
     */
    @Override
    public void forEachDocument(Consumer<IndexedDocument> consumer) throws IOException {
        try (IndexReader reader = DirectoryReader.open(writer, true, true)) {
            LeafReader aReader = SlowCompositeReaderWrapper.wrap(reader);
            Map<String, SortedDocValues> sortedCache = new HashMap<>();
            Map<String, NumericDocValues> numericCache = new HashMap<>();
            Map<String, Boolean> fieldExistsCache = new HashMap<>();
            int maxDoc = aReader.maxDoc();
            for (int doc = 0; doc < maxDoc; doc++) {
                consumer.accept(new LuceneIndexedDocument(aReader, doc, sortedCache, numericCache, fieldExistsCache));
            }
        } catch (IndexNotFoundException e) {
            // index does not yet exist: no callback invocations
        }
    }

    /**
     * {@link IndexedDocument} view backed by a single doc id of an open
     * {@link LeafReader}. Caches the per-field doc-values objects (not
     * per-doc values) across the whole scan, mirroring how
     * {@code SkipCommitedTask}'s previous direct-Lucene code fetched each
     * doc-values field once and advanced it per document.
     */
    private static final class LuceneIndexedDocument implements IndexedDocument {

        private final LeafReader reader;
        private final int doc;
        private final Map<String, SortedDocValues> sortedCache;
        private final Map<String, NumericDocValues> numericCache;
        private final Map<String, Boolean> fieldExistsCache;

        LuceneIndexedDocument(LeafReader reader, int doc, Map<String, SortedDocValues> sortedCache,
                Map<String, NumericDocValues> numericCache, Map<String, Boolean> fieldExistsCache) {
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
            return fieldExistsCache.computeIfAbsent(field, f -> reader.getFieldInfos().fieldInfo(f) != null);
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
