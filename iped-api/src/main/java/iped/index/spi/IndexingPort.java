package iped.index.spi;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Read-side port over a Worker's index, letting a Task query already-indexed
 * state (near-real-time) without depending on {@code org.apache.lucene.*}
 * directly.
 *
 * <p>One instance per Worker (mirroring the Worker instance-per-thread model,
 * see {@code iped.engine.core.Worker}) — never a shared singleton across
 * Workers. Obtained lazily from the owning Worker (e.g.
 * {@code worker.getIndexingPort()}), analogous to how
 * {@link iped.datasource.spi.IDataSourceReader} implementations are
 * obtained/discovered at the module edge rather than referenced by concrete
 * type.
 *
 * <p>This is a proof-of-concept seam (PI-1-F4a-S1): it is deliberately
 * read-only and covers only the single operation actually needed by the
 * sample Task it was designed for. It is not a ServiceLoader SPI (unlike
 * {@code IDataSourceReader}) — the concrete adapter is obtained directly
 * from the Worker for now.
 */
public interface IndexingPort {

    /**
     * Streams the distinct, non-empty stored/doc-values of a single indexed
     * field from the currently-visible (near-real-time) portion of the
     * index.
     *
     * @param field the indexed field name (e.g. {@code IndexItem.HASH})
     * @return a stream of distinct, non-empty field values; empty if the
     *         index does not yet exist
     * @throws IOException if the index cannot be read
     */
    Stream<String> distinctFieldValues(String field) throws IOException;

    /**
     * Scans every currently-visible (near-real-time) document exactly once,
     * invoking {@code consumer} with a read-only {@link IndexedDocument} view
     * of each. Does nothing if the index does not yet exist. The view is
     * valid only within the callback. Callers needing more than one pass may
     * call this method again (each call opens and closes its own NRT
     * reader), exactly as {@link #distinctFieldValues} is safe to call
     * repeatedly.
     *
     * @param consumer callback invoked once per visible document
     * @throws IOException if the index cannot be read
     */
    void forEachDocument(Consumer<IndexedDocument> consumer) throws IOException;
}
