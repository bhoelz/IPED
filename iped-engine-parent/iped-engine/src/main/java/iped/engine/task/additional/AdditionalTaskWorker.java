package iped.engine.task.additional;

import iped.data.IItem;
import iped.engine.core.Worker;

import java.io.File;

/**
 * Minimal {@link Worker} subclass used when executing additional-processing
 * tasks on already-indexed items (post-indexing phase).
 *
 * <h3>Design constraints</h3>
 * <ul>
 *   <li>{@link #writer} is intentionally left {@code null} — additional
 *       processing MUST NOT write to the main Lucene index.</li>
 *   <li>{@link #manager} is intentionally left {@code null} — no queue
 *       management is performed.  {@link iped.engine.task.AbstractTask}
 *       handles {@code null} manager safely.</li>
 *   <li>{@link #stats} is intentionally left {@code null} — statistics are
 *       not collected.  {@link iped.engine.task.AbstractTask} handles
 *       {@code null} stats safely.</li>
 *   <li>{@link #decItemsBeingProcessed()} is overridden as a no-op because
 *       there is no backing queue to decrement.</li>
 *   <li>{@link #processNewItem(IItem)} throws
 *       {@link UnsupportedOperationException} — tasks that create child items
 *       are not supported in additional-processing mode and must not be
 *       annotated with {@link iped.task.AdditionalProcessingCapable}.</li>
 * </ul>
 */
public class AdditionalTaskWorker extends Worker {

    /**
     * @param id     worker identifier (used only for thread naming)
     * @param output case module directory; tasks may use it for auxiliary
     *               output but MUST NOT write to the main index
     */
    public AdditionalTaskWorker(int id, File output) {
        super(id);           // protected constructor added in Worker
        this.output = output;
        // writer  = null (intentional — prevents main-index writes)
        // manager = null (intentional — no queue management)
        // stats   = null (intentional — statistics not collected)
        // caseData = null (intentional — tasks must handle null caseData)
    }

    /** No-op: there is no backing queue to decrement in additional processing. */
    @Override
    public void decItemsBeingProcessed() {
        // intentionally empty
    }

    /**
     * Fails fast: tasks that create new child items are not compatible with
     * post-indexing additional processing.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void processNewItem(IItem evidence) {
        throw new UnsupportedOperationException(
                "Creating new child items is not supported during additional processing. " //$NON-NLS-1$
                + "Do not annotate this task with @AdditionalProcessingCapable."); //$NON-NLS-1$
    }
}
