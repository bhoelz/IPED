package iped.distributed.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iped.distributed.kafka.KafkaItemMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Publishes {@link ItemStatusEvent}s to the global {@code iped.status} Kafka topic.
 *
 * <p>Each Task Agent and each datasource reader holds one instance.
 * Events are sent asynchronously (fire-and-forget) — the status topic is
 * observability infrastructure, not a critical data path.
 */
public class ItemStatusProducer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemStatusProducer.class);

    /** Name of the global status topic. */
    public static final String STATUS_TOPIC = "iped.status";

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper mapper;

    public ItemStatusProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");                // leader ack only
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);             // micro-batching
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        this.producer = new KafkaProducer<>(props);
        this.mapper   = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void publishDiscovered(KafkaItemMessage msg) {
        publish(ItemStatusEvent.discovered(msg));
    }

    public void publishStarted(KafkaItemMessage msg, String taskType) {
        publish(ItemStatusEvent.started(msg, taskType));
    }

    public void publishCompleted(KafkaItemMessage msg, String taskType, long durationMs) {
        publish(ItemStatusEvent.completed(msg, taskType, durationMs));
    }

    public void publishError(KafkaItemMessage msg, String taskType, long durationMs, Throwable cause) {
        publish(ItemStatusEvent.error(msg, taskType, durationMs, cause));
    }

    public void publishSkipped(KafkaItemMessage msg, String taskType) {
        publish(ItemStatusEvent.skipped(msg, taskType));
    }

    public void publishSubitemDiscovered(KafkaItemMessage msg) {
        publish(ItemStatusEvent.subitemDiscovered(msg));
    }

    public void publishCaseCompleted(String caseId) {
        publish(ItemStatusEvent.caseCompleted(caseId));
    }

    private void publish(ItemStatusEvent event) {
        try {
            String json = mapper.writeValueAsString(event);
            // Key = caseId so all events for a case land in the same partition
            producer.send(new ProducerRecord<>(STATUS_TOPIC, event.getCaseId(), json),
                    (meta, ex) -> {
                        if (ex != null) {
                            LOGGER.warn("Failed to publish status event type={} item={}",
                                    event.getType(), event.getItemUuid(), ex);
                        }
                    });
        } catch (Exception e) {
            LOGGER.warn("Could not serialize status event", e);
        }
    }

    @Override
    public void close() {
        producer.close();
    }
}
