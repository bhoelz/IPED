package iped.pipeline;

/**
 * Processing phase of a job lifecycle event.
 */
public enum JobPhase {
    /** The job has been submitted and is waiting for resources. */
    QUEUED,
    /** The job is actively being processed. */
    STARTED,
    /** The job has been suspended; processing is paused. */
    PAUSED,
    /** The job has been resumed after a pause. */
    RESUMED,
    /** All items have been processed successfully. */
    COMPLETED,
    /** The job stopped due to an unrecoverable error. */
    FAILED
}
