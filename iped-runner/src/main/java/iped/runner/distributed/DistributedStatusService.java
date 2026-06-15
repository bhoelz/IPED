package iped.runner.distributed;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import iped.runner.execution.JobSnapshot;
import iped.runner.execution.JobSnapshotProvider;
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
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Consumes the global {@code iped.status} Kafka topic and aggregates per-case progress
 * for the Processing Dashboard.
 *
 * <p>Disabled unless {@code runner.kafka.bootstrap-servers} is set. Each runner instance
 * joins with a unique consumer group and reads the topic from the beginning, so the
 * dashboard rebuilds the full cluster view after a restart without affecting the
 * processing consumer groups.
 */
@Service
@Slf4j
public class DistributedStatusService implements JobSnapshotProvider, AutoCloseable {

    static final String STATUS_TOPIC = "iped.status";

    @Value("${runner.kafka.bootstrap-servers:}")
    private String bootstrapServers;

    @Autowired
    private ProcessingAuditLog auditLog;

    // Jackson 3: java.time is supported out of the box, no JavaTimeModule needed
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final ConcurrentHashMap<String, CaseStats> cases = new ConcurrentHashMap<>();

    private volatile KafkaConsumer<String, String> consumer;
    private volatile boolean running;
    private Thread pollThread;

    @PostConstruct
    void start() {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            log.info("Distributed status monitor disabled (runner.kafka.bootstrap-servers not set)");
            return;
        }
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "iped-runner-dashboard-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumer = new KafkaConsumer<>(props);
        running = true;
        pollThread = new Thread(this::pollLoop, "runner-kafka-status");
        pollThread.setDaemon(true);
        pollThread.start();
        log.info("Distributed status monitor started, topic '{}' on {}", STATUS_TOPIC, bootstrapServers);
    }

    private void pollLoop() {
        try {
            consumer.subscribe(List.of(STATUS_TOPIC));
            while (running) {
                var records = consumer.poll(Duration.ofSeconds(1));
                records.forEach(r -> handleMessage(r.value()));
            }
        } catch (WakeupException e) {
            // expected on close()
        } catch (Exception e) {
            log.error("Status consumer terminated", e);
        } finally {
            consumer.close();
        }
    }

    void handleMessage(String json) {
        StatusEvent event;
        try {
            event = mapper.readValue(json, StatusEvent.class);
        } catch (Exception e) {
            log.debug("Skipping unparseable status event: {}", e.getMessage());
            return;
        }
        if (event.getType() == null || event.getCaseId() == null) return;

        cases.computeIfAbsent(event.getCaseId(),
                        id -> new CaseStats(id, event.getTimestamp()))
                .apply(event);

        auditLog.record(ProcessingRecord.from(event, event.getAgentId()));
    }

    @Override
    public List<JobSnapshot> snapshots() {
        return cases.values().stream()
                .map(CaseStats::toSnapshot)
                .sorted(Comparator.comparing(JobSnapshot::startedAt))
                .toList();
    }

    @PreDestroy
    @Override
    public void close() {
        running = false;
        var c = consumer;
        if (c != null) c.wakeup();
        var t = pollThread;
        if (t != null) {
            try {
                t.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
