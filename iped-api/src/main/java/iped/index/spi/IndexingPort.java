package iped.index.spi;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Read-side port over a Worker's index, letting a Task query already-indexed state (near-real-time)
 * without depending on {@code org.apache.lucene.*} directly.
 *
 * <p>One instance per Worker (mirroring the Worker instance-per-thread model, see {@code
 * iped.engine.core.Worker}) — never a shared singleton across Workers. Obtained lazily from the
 * owning Worker (e.g. {@code worker.getIndexingPort()}), analogous to how {@link
 * iped.datasource.spi.IDataSourceReader} implementations are obtained/discovered at the module edge
 * rather than referenced by concrete type.
 *
 * <p>This is a proof-of-concept seam (PI-1-F4a-S1): it is deliberately read-only and covers only
 * the single operation actually needed by the sample Task it was designed for. It is not a
 * ServiceLoader SPI (unlike {@code IDataSourceReader}) — the concrete adapter is obtained directly
 * from the Worker for now.
 */
public interface IndexingPort {

  /**
   * Streams the distinct, non-empty stored/doc-values of a single indexed field from the
   * currently-visible (near-real-time) portion of the index.
   *
   * @param field the indexed field name (e.g. {@code IndexItem.HASH})
   * @return a stream of distinct, non-empty field values; empty if the index does not yet exist
   * @throws IOException if the index cannot be read
   */
  Stream<String> distinctFieldValues(String field) throws IOException;

  /**
   * Scans every currently-visible (near-real-time) document exactly once, invoking {@code consumer}
   * with a read-only {@link IndexedDocument} view of each. Does nothing if the index does not yet
   * exist. The view is valid only within the callback. Callers needing more than one pass may call
   * this method again (each call opens and closes its own NRT reader), exactly as {@link
   * #distinctFieldValues} is safe to call repeatedly.
   *
   * @param consumer callback invoked once per visible document
   * @throws IOException if the index cannot be read
   */
  void forEachDocument(Consumer<IndexedDocument> consumer) throws IOException;

  /**
   * Opens a single near-real-time reader, hands an {@link IndexingSession} bound to it to {@code
   * sessionConsumer}, then closes the reader. Lets a caller that needs several sequential field
   * queries/scans (each equivalent to one {@link #forEachDocument}/{@link #distinctFieldValues}
   * call) do so against ONE open reader, instead of one open/close per call.
   *
   * <p>Prefer this over several separate {@link #forEachDocument}/ {@link #distinctFieldValues}
   * calls whenever more than one such call is made within the same logical unit of work (e.g. a
   * Task's {@code init()}).
   *
   * @param sessionConsumer callback invoked once with the open session
   * @throws IOException if the index cannot be read
   */
  void withSession(SessionConsumer sessionConsumer) throws IOException;

  /**
   * Callback for {@link #withSession}; a {@link Consumer}-like functional interface that is allowed
   * to throw {@link IOException}, mirroring the checked-exception contracts of {@link
   * IndexingSession}'s methods.
   */
  @FunctionalInterface
  interface SessionConsumer {
    void accept(IndexingSession session) throws IOException;
  }
}
