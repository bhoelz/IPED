package iped.distributed.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka Deserializer for {@link KafkaItemMessage} — reads from UTF-8 JSON bytes.
 */
public class KafkaItemDeserializer implements Deserializer<KafkaItemMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaItemDeserializer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public KafkaItemMessage deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return MAPPER.readValue(data, KafkaItemMessage.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize KafkaItemMessage from topic {}", topic, e);
            throw new RuntimeException("KafkaItemMessage deserialization failed", e);
        }
    }
}
