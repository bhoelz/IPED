package iped.distributed.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka Serializer for {@link KafkaItemMessage} — converts to UTF-8 JSON bytes.
 */
public class KafkaItemSerializer implements Serializer<KafkaItemMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaItemSerializer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public byte[] serialize(String topic, KafkaItemMessage data) {
        if (data == null) return null;
        try {
            return MAPPER.writeValueAsBytes(data);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize KafkaItemMessage for item {}", data.getItemUuid(), e);
            throw new RuntimeException("KafkaItemMessage serialization failed", e);
        }
    }
}
