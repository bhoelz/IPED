package iped.runner.distributed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Kafka message on the {@code iped.agents} heartbeat topic.
 *
 * <p>Agents publish this periodically (typically every 10–30 s) so the runner
 * dashboard can detect stale or offline agents without a dedicated RPC channel.
 * Fields are optional — older agents may omit {@code version}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentHeartbeatEvent(
        String  agentId,
        String  hostname,
        Instant startedAt,
        Instant lastSeen,
        String  version,
        int     activeTasks,
        int     maxTasks
) {
    public static final String TOPIC = "iped.agents";
}
