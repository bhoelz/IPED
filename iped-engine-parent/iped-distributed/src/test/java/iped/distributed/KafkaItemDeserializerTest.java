package iped.distributed;

import iped.distributed.kafka.KafkaItemDeserializer;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.KafkaItemSerializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link KafkaItemDeserializer} poison-message handling.
 *
 * <p>None of these tests require a running Kafka broker; they exercise only the
 * deserializer's parse-then-sentinel logic.
 */
class KafkaItemDeserializerTest {

    private static final String TOPIC = "iped.case1.stage.3";
    private final KafkaItemDeserializer deserializer = new KafkaItemDeserializer();

    // -------------------------------------------------------------------------
    // Null / empty input
    // -------------------------------------------------------------------------

    @Test
    void nullBytesReturnsNull() {
        assertNull(deserializer.deserialize(TOPIC, null));
    }

    // -------------------------------------------------------------------------
    // Valid messages
    // -------------------------------------------------------------------------

    @Test
    void validJsonRoundTripsCorrectly() throws Exception {
        KafkaItemMessage original = new KafkaItemMessage();
        original.setItemUuid("abc-123");
        original.setPath("/evidence/file.doc");
        original.setPipelineStage(3);

        byte[] bytes = new KafkaItemSerializer().serialize(TOPIC, original);
        KafkaItemMessage deserialized = deserializer.deserialize(TOPIC, bytes);

        assertNotNull(deserialized);
        assertFalse(KafkaItemDeserializer.isPoison(deserialized));
        assertEquals("abc-123", deserialized.getItemUuid());
        assertEquals("/evidence/file.doc", deserialized.getPath());
        assertEquals(3, deserialized.getPipelineStage());
    }

    // -------------------------------------------------------------------------
    // Poison cases — deserializer must never throw
    // -------------------------------------------------------------------------

    @Test
    void malformedJsonReturnsSentinelNotException() {
        byte[] garbage = "{ not valid json !!!".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = assertDoesNotThrow(
                () -> deserializer.deserialize(TOPIC, garbage));
        assertNotNull(sentinel, "must return a sentinel, not null");
        assertTrue(KafkaItemDeserializer.isPoison(sentinel));
    }

    @Test
    void truncatedJsonReturnsSentinel() {
        byte[] truncated = "{\"itemUuid\":\"x\",\"path\":".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = assertDoesNotThrow(
                () -> deserializer.deserialize(TOPIC, truncated));
        assertTrue(KafkaItemDeserializer.isPoison(sentinel));
    }

    @Test
    void emptyBytesReturnsSentinel() {
        byte[] empty = new byte[0];
        KafkaItemMessage sentinel = assertDoesNotThrow(
                () -> deserializer.deserialize(TOPIC, empty));
        assertTrue(KafkaItemDeserializer.isPoison(sentinel));
    }

    @Test
    void wrongTypeJsonReturnsSentinel() {
        // A JSON array is not a KafkaItemMessage — Jackson will fail to map it
        byte[] array = "[1,2,3]".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = assertDoesNotThrow(
                () -> deserializer.deserialize(TOPIC, array));
        assertTrue(KafkaItemDeserializer.isPoison(sentinel));
    }

    @Test
    void randomBinaryReturnsSentinel() {
        byte[] binary = new byte[200];
        for (int i = 0; i < binary.length; i++) binary[i] = (byte) (i ^ 0xAB);
        KafkaItemMessage sentinel = assertDoesNotThrow(
                () -> deserializer.deserialize(TOPIC, binary));
        assertTrue(KafkaItemDeserializer.isPoison(sentinel));
    }

    // -------------------------------------------------------------------------
    // Sentinel structure
    // -------------------------------------------------------------------------

    @Test
    void sentinelHasNonNullUuid() {
        byte[] garbage = "NOTJSON".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(TOPIC, garbage);
        assertNotNull(sentinel.getItemUuid(), "sentinel must carry a UUID for DLQ routing");
        assertFalse(sentinel.getItemUuid().isBlank());
    }

    @Test
    void sentinelPathContainsTopic() {
        byte[] garbage = "NOTJSON".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(TOPIC, garbage);
        assertTrue(sentinel.getPath().contains(TOPIC),
                "path should reference the source topic for operator visibility");
    }

    @Test
    void sentinelPipelineStageMatchesTopicSuffix() {
        KafkaItemMessage sentinel =
                deserializer.deserialize("iped.case-x.stage.7", "bad".getBytes(StandardCharsets.UTF_8));
        assertEquals(7, sentinel.getPipelineStage());
    }

    @Test
    void sentinelPipelineStageIsMinusOneForNonStandardTopic() {
        KafkaItemMessage sentinel =
                deserializer.deserialize("iped.status", "bad".getBytes(StandardCharsets.UTF_8));
        assertEquals(-1, sentinel.getPipelineStage());
    }

    @Test
    void sentinelCarriesPoisonErrorKey() {
        byte[] garbage = "NOTJSON".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(TOPIC, garbage);
        Map<String, Object> attrs = sentinel.getExtraAttributes();
        assertNotNull(attrs);
        assertTrue(attrs.containsKey(KafkaItemDeserializer.POISON_ERROR_KEY));
        assertNotNull(attrs.get(KafkaItemDeserializer.POISON_ERROR_KEY));
    }

    @Test
    void sentinelCarriesRawSnippetKey() {
        byte[] garbage = "NOTJSON".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(TOPIC, garbage);
        Map<String, Object> attrs = sentinel.getExtraAttributes();
        assertNotNull(attrs);
        assertTrue(attrs.containsKey(KafkaItemDeserializer.POISON_RAW_SNIPPET_KEY),
                "sentinel must include a hex snippet for operator inspection");
        String snippet = (String) attrs.get(KafkaItemDeserializer.POISON_RAW_SNIPPET_KEY);
        assertFalse(snippet.isBlank());
        // Hex characters only (possibly followed by the ellipsis marker)
        assertTrue(snippet.matches("[0-9a-f…()0-9 bytes total]+"),
                "snippet should be hex-encoded: " + snippet);
    }

    @Test
    void rawSnippetIsTruncatedForLargePayloads() {
        // Payload larger than RAW_SNIPPET_MAX_BYTES (100) — snippet must be capped
        byte[] large = new byte[500];
        KafkaItemMessage sentinel = deserializer.deserialize(TOPIC, large);
        String snippet = (String) sentinel.getExtraAttributes()
                .get(KafkaItemDeserializer.POISON_RAW_SNIPPET_KEY);
        // 100 bytes = 200 hex chars, plus ellipsis marker
        assertTrue(snippet.contains("…") || snippet.length() <= KafkaItemDeserializer.RAW_SNIPPET_MAX_BYTES * 2 + 50,
                "snippet should be capped for large payloads");
        assertTrue(snippet.contains("500 bytes total"), "total byte count should be reported");
    }

    @Test
    void rawSnippetIsNotTruncatedForSmallPayloads() {
        byte[] small = "abc".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(TOPIC, small);
        String snippet = (String) sentinel.getExtraAttributes()
                .get(KafkaItemDeserializer.POISON_RAW_SNIPPET_KEY);
        assertFalse(snippet.contains("…"), "small payloads should not be truncated");
    }

    // -------------------------------------------------------------------------
    // isPoison — guard method
    // -------------------------------------------------------------------------

    @Test
    void isPoisonReturnsFalseForNull() {
        assertFalse(KafkaItemDeserializer.isPoison(null));
    }

    @Test
    void isPoisonReturnsFalseForNormalMessage() {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setItemUuid("x");
        assertFalse(KafkaItemDeserializer.isPoison(msg));
    }

    @Test
    void isPoisonReturnsFalseForMessageWithUnrelatedExtraAttributes() {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setExtraAttributes(Map.of("custom.key", "value"));
        assertFalse(KafkaItemDeserializer.isPoison(msg));
    }

    @Test
    void isPoisonReturnsTrueForSentinel() {
        byte[] garbage = "NOTJSON".getBytes(StandardCharsets.UTF_8);
        KafkaItemMessage sentinel = deserializer.deserialize(TOPIC, garbage);
        assertTrue(KafkaItemDeserializer.isPoison(sentinel));
    }

    @Test
    void differentDeserializerInstancesProducePoisonSentinels() {
        byte[] garbage = "???".getBytes(StandardCharsets.UTF_8);
        KafkaItemDeserializer d1 = new KafkaItemDeserializer();
        KafkaItemDeserializer d2 = new KafkaItemDeserializer();
        assertTrue(KafkaItemDeserializer.isPoison(d1.deserialize(TOPIC, garbage)));
        assertTrue(KafkaItemDeserializer.isPoison(d2.deserialize(TOPIC, garbage)));
    }
}
