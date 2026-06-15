package iped.runner.distributed;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes the {@code iped.agents} Kafka heartbeat topic and maintains a live
 * inventory of remote processing agents with computed health status.
 *
 * <p>Disabled unless {@code runner.kafka.bootstrap-servers} is set (same condition
 * as {@link DistributedStatusService}). Uses its own consumer group so agent
 * heartbeats are read independently from case-status events.
 */
@Service
@Slf4j
public class AgentInventoryService implements AutoCloseable {

    @Value("${runner.kafka.bootstrap-servers:}")
    private String bootstrapServers;

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final ConcurrentHashMap<String, AgentHeartbeatEvent> lastHeartbeats =
            new ConcurrentHashMap<>();

    private volatile KafkaConsumer<String, String> consumer;
    private volatile boolean running;
    private Thread pollThread;

    @PostConstruct
    void start() {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            log.info("Agent inventory disabled (runner.kafka.bootstrap-servers not set)");
            return;
        }
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "iped-runner-agents-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumer = new KafkaConsumer<>(props);
        running = true;
        pollThread = new Thread(this::pollLoop, "runner-kafka-agents");
        pollThread.setDaemon(true);
        pollThread.start();
        log.info("Agent inventory consumer started on topic '{}', {}",
                AgentHeartbeatEvent.TOPIC, bootstrapServers);
    }

    private void pollLoop() {
        try {
            consumer.subscribe(List.of(AgentHeartbeatEvent.TOPIC));
            while (running) {
                consumer.poll(Duration.ofSeconds(1))
                        .forEach(r -> handleMessage(r.value()));
            }
        } catch (WakeupException e) {
            // expected on close()
        } catch (Exception e) {
            log.error("Agent inventory consumer terminated unexpectedly", e);
        } finally {
            consumer.close();
        }
    }

    void handleMessage(String json) {
        AgentHeartbeatEvent event;
        try {
            event = mapper.readValue(json, AgentHeartbeatEvent.class);
        } catch (Exception e) {
            log.debug("Skipping unparseable agent heartbeat: {}", e.getMessage());
            return;
        }
        if (event.agentId() == null) return;

        // Always keep the latest heartbeat per agent
        lastHeartbeats.merge(event.agentId(), event, (existing, incoming) ->
                incoming.lastSeen() != null && existing.lastSeen() != null &&
                incoming.lastSeen().isAfter(existing.lastSeen()) ? incoming : existing);
    }

    /** Returns all known agents with health status computed at call time. */
    public List<AgentRecord> listAgents() {
        Instant now = Instant.now();
        return lastHeartbeats.values().stream()
                .map(h -> toRecord(h, now))
                .sorted(Comparator.comparing(AgentRecord::agentId))
                .toList();
    }

    /** Returns true when Kafka is configured and the consumer is running. */
    public boolean isEnabled() {
        return running && pollThread != null && pollThread.isAlive();
    }

    private AgentRecord toRecord(AgentHeartbeatEvent h, Instant now) {
        Instant seen = h.lastSeen() != null ? h.lastSeen() : Instant.EPOCH;
        return new AgentRecord(
                h.agentId(), h.hostname(), h.startedAt(), seen,
                h.version(), h.activeTasks(), h.maxTasks(),
                AgentRecord.computeHealth(seen, now));
    }

    @PreDestroy
    @Override
    public void close() {
        running = false;
        var c = consumer;
        if (c != null) c.wakeup();
        var t = pollThread;
        if (t != null) {
            try { t.join(5000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
