package iped.distributed.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Kafka Deserializer for {@link KafkaItemMessage} — reads from UTF-8 JSON bytes.
 */
@Slf4j
public class KafkaItemDeserializer implements Deserializer<KafkaItemMessage> {


    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public KafkaItemMessage deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return MAPPER.readValue(data, KafkaItemMessage.class);
        } catch (Exception e) {
            log.error("Failed to deserialize KafkaItemMessage from topic {}", topic, e);
            throw new RuntimeException("KafkaItemMessage deserialization failed", e);
        }
    }
}
