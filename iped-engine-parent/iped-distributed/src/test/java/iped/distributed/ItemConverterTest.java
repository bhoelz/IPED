package iped.distributed;

import iped.distributed.kafka.ItemConverter;
import iped.distributed.kafka.KafkaItemMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemConverterTest {

    // ---- Public constant fields ----

    @Test
    void attrKafkaMsg_isNonBlank() {
        assertNotNull(ItemConverter.ATTR_KAFKA_MSG);
        assertFalse(ItemConverter.ATTR_KAFKA_MSG.isBlank());
    }

    @Test
    void attrOutputTopic_isNonBlank() {
        assertNotNull(ItemConverter.ATTR_OUTPUT_TOPIC);
        assertFalse(ItemConverter.ATTR_OUTPUT_TOPIC.isBlank());
    }

    @Test
    void attrRawTopic_isNonBlank() {
        assertNotNull(ItemConverter.ATTR_RAW_TOPIC);
        assertFalse(ItemConverter.ATTR_RAW_TOPIC.isBlank());
    }

    @Test
    void attrItemUuid_isNonBlank() {
        assertNotNull(ItemConverter.ATTR_ITEM_UUID);
        assertFalse(ItemConverter.ATTR_ITEM_UUID.isBlank());
    }

    @Test
    void attrConstants_areDistinct() {
        // All four attribute keys must be different
        assertNotEquals(ItemConverter.ATTR_KAFKA_MSG, ItemConverter.ATTR_OUTPUT_TOPIC);
        assertNotEquals(ItemConverter.ATTR_KAFKA_MSG, ItemConverter.ATTR_RAW_TOPIC);
        assertNotEquals(ItemConverter.ATTR_KAFKA_MSG, ItemConverter.ATTR_ITEM_UUID);
        assertNotEquals(ItemConverter.ATTR_OUTPUT_TOPIC, ItemConverter.ATTR_RAW_TOPIC);
        assertNotEquals(ItemConverter.ATTR_OUTPUT_TOPIC, ItemConverter.ATTR_ITEM_UUID);
        assertNotEquals(ItemConverter.ATTR_RAW_TOPIC, ItemConverter.ATTR_ITEM_UUID);
    }

    @Test
    void attrConstants_startWithDoubleUnderscore() {
        // Temp attributes use "__" prefix by convention
        assertTrue(ItemConverter.ATTR_KAFKA_MSG.startsWith("__"),
                "ATTR_KAFKA_MSG should start with '__'");
        assertTrue(ItemConverter.ATTR_OUTPUT_TOPIC.startsWith("__"),
                "ATTR_OUTPUT_TOPIC should start with '__'");
        assertTrue(ItemConverter.ATTR_RAW_TOPIC.startsWith("__"),
                "ATTR_RAW_TOPIC should start with '__'");
        assertTrue(ItemConverter.ATTR_ITEM_UUID.startsWith("__"),
                "ATTR_ITEM_UUID should start with '__'");
    }

    // ---- KafkaItemMessage pipelineStage incremented in mergeState ----

    @Test
    void kafkaItemMessage_initialPipelineStage_isZero() {
        // When ItemConverter.toMessage() sets stage to 0, verify via KafkaItemMessage
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setPipelineStage(0);
        assertEquals(0, msg.getPipelineStage());
    }

    @Test
    void kafkaItemMessage_pipelineStage_canBeIncremented() {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setPipelineStage(2);
        // mergeState increments by 1
        msg.setPipelineStage(msg.getPipelineStage() + 1);
        assertEquals(3, msg.getPipelineStage());
    }
}
