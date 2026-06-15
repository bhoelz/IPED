package iped.runner.execution;

/** Terminal and in-flight states for an IPED process managed by the runner. */
public enum RunStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    ABORTED,
    TIMED_OUT
}
