package iped.engine.lucene;

import iped.index.spi.IndexingPort;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SortedDocValues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
}
