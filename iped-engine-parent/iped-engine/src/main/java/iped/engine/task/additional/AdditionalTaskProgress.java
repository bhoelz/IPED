package iped.engine.task.additional;

/**
 * Immutable snapshot of the progress of an additional-processing run.
 * Instances are delivered to the progress callback registered with
 * {@link AdditionalTaskRunner#run}.
 */
public final class AdditionalTaskProgress {

    private final String taskName;
    private final int    processed;
    private final int    total;
    private final int    errors;

    public AdditionalTaskProgress(String taskName, int processed, int total, int errors) {
        this.taskName  = taskName;
        this.processed = processed;
        this.total     = total;
        this.errors    = errors;
    }

    /** @return human-readable name of the task being executed */
    public String getTaskName() { return taskName; }

    /** @return number of items successfully processed so far */
    public int getProcessed() { return processed; }

    /** @return total number of items to process */
    public int getTotal() { return total; }

    /** @return number of items that failed (logged but not fatal) */
    public int getErrors() { return errors; }

    /** @return {@code true} when all items have been processed or errored */
    public boolean isDone() { return processed + errors >= total; }

    /** @return progress as a value in {@code [0.0, 1.0]} */
    public double getProgressFraction() {
        return total > 0 ? (double) (processed + errors) / total : 0.0;
    }
}
