package iped.runner.execution;

import java.time.Instant;

/** Immutable record of a completed, failed, or aborted run retained in history. */
public record RunSummary(
    String id,
    String name,
    String source,
    RunStatus status,
    Instant startedAt,
    Instant endedAt,
    int exitCode,
    long itemsProcessed,
    long itemsFound) {}
