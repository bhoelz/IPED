package iped.index.spi;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Read-side view of a single already-open near-real-time reader, handed to
 * an {@link IndexingPort#withSession} callback. Lets a caller issue several
 * field queries/scans (each equivalent to one {@link IndexingPort} call)
 * against the SAME open reader, instead of each call opening/closing its
 * own NRT {@code DirectoryReader}.
 *
 * <p>Introduced to fix a performance regression: a Task issuing several
 * sequential {@code IndexingPort.forEachDocument}/{@code distinctFieldValues}
 * calls within one logical unit of work (e.g. {@code Task.init()}) was
 * paying for one NRT reader open per call instead of one for the whole unit
 * of work.
 *
 * <p>Valid only for the duration of the {@code IndexingPort.withSession}
 * callback that receives it — do not retain the instance past the callback
 * (the underlying reader is closed when the session ends).
 */
public interface IndexingSession {

    /**
     * Same contract as {@link IndexingPort#distinctFieldValues(String)}, but
     * reusing this session's already-open reader instead of opening a new one.
     */
    Stream<String> distinctFieldValues(String field) throws IOException;

    /**
     * Same contract as {@link IndexingPort#forEachDocument(Consumer)}, but
     * reusing this session's already-open reader instead of opening a new one.
     */
    void forEachDocument(Consumer<IndexedDocument> consumer) throws IOException;

    /**
     * Whether {@code field} is present anywhere in this session's index
     * schema (via field metadata, not per-document values), letting a caller
     * short-circuit a full {@link #forEachDocument} scan when a field is
     * provably absent index-wide (e.g. a subitem-count field before any item
     * has produced it). {@code false} if the index does not yet exist.
     */
    boolean fieldExists(String field);
}
