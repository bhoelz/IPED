package iped.runner.distributed;

import java.time.Instant;

/**
 * Current known state of a remote processing agent.
 *
 * <p>Health thresholds:
 *
 * <ul>
 *   <li>{@code active} — last heartbeat within 30 s
 *   <li>{@code stale} — 30–120 s ago
 *   <li>{@code offline} — more than 120 s ago
 * </ul>
 */
public record AgentRecord(
    String agentId,
    String hostname,
    Instant startedAt,
    Instant lastSeen,
    String version,
    int activeTasks,
    int maxTasks,
    String health // "active" | "stale" | "offline"
    ) {
  static final long STALE_SECS = 30;
  static final long OFFLINE_SECS = 120;

  static String computeHealth(Instant lastSeen, Instant now) {
    long age = java.time.Duration.between(lastSeen, now).toSeconds();
    if (age < STALE_SECS) return "active";
    if (age < OFFLINE_SECS) return "stale";
    return "offline";
  }

  /** Returns a copy of this record with health recomputed against {@code now}. */
  AgentRecord withHealth(Instant now) {
    return new AgentRecord(
        agentId,
        hostname,
        startedAt,
        lastSeen,
        version,
        activeTasks,
        maxTasks,
        computeHealth(lastSeen, now));
  }
}
