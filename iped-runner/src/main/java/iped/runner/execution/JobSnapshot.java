package iped.runner.execution;

import java.time.Instant;
import java.util.List;

/** Immutable point-in-time view of a running or completed IPED job, served to the dashboard. */
public record JobSnapshot(
    String id,
    String name,
    String source,
    String status,
    Instant startedAt,
    long durationMs,
    long itemsProcessed,
    long itemsFound,
    String currentSpeed,
    String eta,
    List<String> recentLines) {}
