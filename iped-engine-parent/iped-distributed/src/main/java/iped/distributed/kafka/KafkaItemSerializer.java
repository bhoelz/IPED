package iped.distributed.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka Serializer for {@link KafkaItemMessage} — converts to UTF-8 JSON bytes.
 */
@Slf4j
public class KafkaItemSerializer implements Serializer<KafkaItemMessage> {


    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public byte[] serialize(String topic, KafkaItemMessage data) {
        if (data == null) return null;
        try {
            return MAPPER.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("Failed to serialize KafkaItemMessage for item {}", data.getItemUuid(), e);
            throw new RuntimeException("KafkaItemMessage serialization failed", e);
        }
    }
}
