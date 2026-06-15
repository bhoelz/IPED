package iped.runner.execution;

import java.time.Instant;

/**
 * Immutable token representing a run waiting in the priority queue.
 * Once dispatched, the actual live run is tracked by {@link RunRecord}.
 */
public record QueuedRun(
        String id,
        RunRequest request,
        RunPriority priority,
        Instant enqueuedAt
) {}
