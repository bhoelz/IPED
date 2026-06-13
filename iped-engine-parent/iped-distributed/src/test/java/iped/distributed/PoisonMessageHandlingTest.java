package iped.distributed;

import iped.distributed.kafka.KafkaItemDeserializer;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.KafkaItemSerializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the poison-message handling path.
 *
 * <p>These tests verify the <em>full chain</em>: malformed bytes enter the
 * {@link KafkaItemDeserializer}, a sentinel is produced, and
 * {@link KafkaItemDeserializer#isPoison} correctly identifies it for DLQ routing
 * by {@link iped.distributed.agent.TaskAgent#processRecord}.
 *
 * <p>No Kafka broker is required — the tests exercise only the in-process logic.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>A poison sentinel is <em>never</em> {@code null}.</li>
 *   <li>A poison sentinel always has a non-null UUID (for DLQ keying).</li>
 *   <li>{@link KafkaItemDeserializer#isPoison} is the single point of truth for
 *       whether {@code processRecord} should skip task execution.</li>
 *   <li>A valid message that happens to have extra attributes is NOT flagged as
 *       poison unless it carries the specific {@link KafkaItemDeserializer#POISON_ERROR_KEY}.</li>
 * </ul>
 */
class PoisonMessageHandlingTest {

    private static final String STAGE_TOPIC = "iped.caseABC.stage.5";
    private final KafkaItemDeserializer deserializer = new KafkaItemDeserializer();

    // -------------------------------------------------------------------------
    // Full chain: malformed bytes → sentinel → detected as poison
    // -------------------------------------------------------------------------

    @Test
    void malformedBytesProduceDetectablePoison() {
        byte[] malformed = "{bad".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage result = deserializer.deserialize(STAGE_TOPIC, malformed);
        assertNotNull(result, "deserializer must return a sentinel, never null");
        assertTrue(KafkaItemDeserializer.isPoison(result),
                "sentinel produced from malformed bytes must be detected as poison");
    }

    @Test
    void emptyBytesProduceDetectablePoison() {
        KafkaItemMessage result = deserializer.deserialize(STAGE_TOPIC, new byte[0]);
        assertNotNull(result);
        assertTrue(KafkaItemDeserializer.isPoison(result));
    }

    @Test
    void validMessageIsNotPoison() throws Exception {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setItemUuid("real-uuid");
        msg.setPath("/evidence/file.bin");
        msg.setPipelineStage(5);

        byte[] bytes = new KafkaItemSerializer().serialize(STAGE_TOPIC, msg);
        KafkaItemMessage deserialized = deserializer.deserialize(STAGE_TOPIC, bytes);

        assertFalse(KafkaItemDeserializer.isPoison(deserialized),
                "a properly serialized message must never be flagged as poison");
    }

    // -------------------------------------------------------------------------
    // Sentinel is suitable for DLQ routing
    // -------------------------------------------------------------------------

    @Test
    void sentinelHasUuidSuitableForDlqKey() {
        byte[] garbage = "0xDEADBEEF".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(STAGE_TOPIC, garbage);
        assertNotNull(sentinel.getItemUuid());
        assertFalse(sentinel.getItemUuid().isBlank(),
                "DLQ producer requires a non-blank key");
    }

    @Test
    void sentinelHasPipelineStageForDlqTopicDerivation() {
        // The DLQ topic name is derived from inputTopic = stageTopic(caseId, stageNumber)
        // processRecord passes `inputTopic + dlqSuffix` to sendToDeadLetterQueue; the sentinel's
        // pipelineStage is informational only, but should match the topic.
        KafkaItemMessage sentinel =
                deserializer.deserialize("iped.caseABC.stage.5", "bad".getBytes(StandardCharsets.UTF_8));
        assertEquals(5, sentinel.getPipelineStage(),
                "sentinel's pipelineStage should match the topic's stage number");
    }

    @Test
    void sentinelErrorMessageIsReadable() {
        byte[] garbage = "not-json-at-all".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(STAGE_TOPIC, garbage);
        Object errValue = sentinel.getExtraAttributes().get(KafkaItemDeserializer.POISON_ERROR_KEY);
        assertNotNull(errValue, "POISON_ERROR_KEY must be present");
        String errMsg = errValue.toString();
        assertFalse(errMsg.isBlank(),
                "error message must not be blank — operators rely on it for diagnosis");
    }

    // -------------------------------------------------------------------------
    // Partition continuity — subsequent valid records after a poison one must
    // round-trip correctly through the deserializer (the deserializer is stateless).
    // -------------------------------------------------------------------------

    @Test
    void validMessageAfterPoisonRoundTripsCorrectly() throws Exception {
        byte[] poison = "BROKEN".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(STAGE_TOPIC, poison);
        assertTrue(KafkaItemDeserializer.isPoison(sentinel));

        // Now deserialize a valid message on the same topic
        KafkaItemMessage valid = new KafkaItemMessage();
        valid.setItemUuid("next-item-uuid");
        valid.setPath("/evidence/next.pdf");
        byte[] validBytes = new KafkaItemSerializer().serialize(STAGE_TOPIC, valid);
        KafkaItemMessage deserialized = deserializer.deserialize(STAGE_TOPIC, validBytes);

        assertFalse(KafkaItemDeserializer.isPoison(deserialized),
                "valid message after poison must not be affected by prior deserialization failure");
        assertEquals("next-item-uuid", deserialized.getItemUuid());
    }

    @Test
    void multiplePoisonMessagesInSequenceAllDetected() {
        byte[][] payloads = {
            "BAD1".getBytes(StandardCharsets.UTF_8),
            "BAD2".getBytes(StandardCharsets.UTF_8),
            new byte[0],
            "{incomplete".getBytes(StandardCharsets.UTF_8)
        };
        for (int i = 0; i < payloads.length; i++) {
            KafkaItemMessage result = deserializer.deserialize(STAGE_TOPIC, payloads[i]);
            assertTrue(KafkaItemDeserializer.isPoison(result),
                    "poison message #" + i + " was not detected");
        }
    }

    // -------------------------------------------------------------------------
    // Guard method — isPoison boundary cases
    // -------------------------------------------------------------------------

    @Test
    void isPoisonReturnsFalseForMessageWithEmptyExtraAttributes() {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setExtraAttributes(new java.util.HashMap<>());
        assertFalse(KafkaItemDeserializer.isPoison(msg));
    }

    @Test
    void isPoisonReturnsFalseForManuallyConstructedMessageWithUnrelatedKey() {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setExtraAttributes(java.util.Map.of("__some.other.key", "value"));
        assertFalse(KafkaItemDeserializer.isPoison(msg),
                "only the exact POISON_ERROR_KEY triggers the poison flag");
    }

    @Test
    void poisonErrorKeyConstantMatchesUsedKey() {
        // White-box: sentinel is built with POISON_ERROR_KEY; guard checks for POISON_ERROR_KEY.
        // If someone changes one without the other, this test catches it.
        byte[] bad = "x".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(STAGE_TOPIC, bad);
        assertTrue(sentinel.getExtraAttributes().containsKey(KafkaItemDeserializer.POISON_ERROR_KEY),
                "sentinel must be keyed with the public constant");
    }
}
