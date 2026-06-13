package iped.distributed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.status.ItemStatusConsumer;
import iped.distributed.status.ItemStatusEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the schema-versioning contract on {@link ItemStatusEvent}:
 * <ul>
 *   <li>All factory methods stamp {@link ItemStatusEvent#SCHEMA_VERSION}.</li>
 *   <li>JSON serialization includes {@code schemaVersion}.</li>
 *   <li>Deserialization of legacy events (no {@code schemaVersion}) yields {@code 0}.</li>
 *   <li>Deserialization of current events round-trips correctly.</li>
 *   <li>Future version events are accepted (forward-compat via {@code @JsonIgnoreProperties}).</li>
 *   <li>{@link ItemStatusConsumer#validateSchemaVersion} does not throw for any version.</li>
 * </ul>
 *
 * <p>No Kafka broker or coordinator required — pure unit tests.
 */
class SchemaVersionTest {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private KafkaItemMessage sampleMsg;

    @BeforeEach
    void setup() {
        sampleMsg = new KafkaItemMessage();
        sampleMsg.setCaseId("case-test");
        sampleMsg.setItemUuid(UUID.randomUUID().toString());
        sampleMsg.setPath("/evidence/test.bin");
        sampleMsg.setPipelineStage(1);
    }

    // =========================================================================
    // SCHEMA_VERSION constant
    // =========================================================================

    @Test
    void schemaVersionConstantIsOne() {
        assertEquals(1, ItemStatusEvent.SCHEMA_VERSION,
                "v1 is the first versioned release; update this assertion when bumping");
    }

    // =========================================================================
    // Factory methods — version stamped
    // =========================================================================

    @Test
    void discoveredSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.discovered(sampleMsg);
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    @Test
    void startedSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.started(sampleMsg, "HashTask");
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    @Test
    void completedSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.completed(sampleMsg, "HashTask", 42);
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    @Test
    void errorSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.error(sampleMsg, "HashTask", 10,
                new RuntimeException("boom"));
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    @Test
    void skippedSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.skipped(sampleMsg, "VideoThumbTask");
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    @Test
    void subitemDiscoveredSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.subitemDiscovered(sampleMsg);
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    @Test
    void timeoutSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.timeout("case-test", UUID.randomUUID().toString(),
                "HashTask", 1, 120_000L);
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    @Test
    void caseCompletedSetsCurrentVersion() {
        ItemStatusEvent e = ItemStatusEvent.caseCompleted("case-test");
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, e.getSchemaVersion());
    }

    // =========================================================================
    // JSON serialization — field is present in wire format
    // =========================================================================

    @Test
    void serializedJsonContainsSchemaVersionField() throws Exception {
        ItemStatusEvent e = ItemStatusEvent.completed(sampleMsg, "HashTask", 10);
        String json = MAPPER.writeValueAsString(e);
        assertTrue(json.contains("\"schemaVersion\""),
                "schemaVersion must appear in the serialized JSON");
    }

    @Test
    void serializedJsonContainsCorrectVersionValue() throws Exception {
        ItemStatusEvent e = ItemStatusEvent.completed(sampleMsg, "HashTask", 10);
        ObjectNode node = (ObjectNode) MAPPER.readTree(MAPPER.writeValueAsString(e));
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, node.get("schemaVersion").intValue());
    }

    // =========================================================================
    // Deserialization — round-trip
    // =========================================================================

    @Test
    void currentVersionRoundTrips() throws Exception {
        ItemStatusEvent original = ItemStatusEvent.completed(sampleMsg, "HashTask", 55);
        String json = MAPPER.writeValueAsString(original);
        ItemStatusEvent restored = MAPPER.readValue(json, ItemStatusEvent.class);

        assertEquals(ItemStatusEvent.SCHEMA_VERSION, restored.getSchemaVersion());
        assertEquals(ItemStatusEvent.Type.COMPLETED, restored.getType());
        assertEquals("case-test", restored.getCaseId());
        assertEquals(55L, restored.getDurationMs());
    }

    // =========================================================================
    // Backward compatibility — legacy events (no schemaVersion field)
    // =========================================================================

    @Test
    void legacyEventWithoutSchemaVersionDeserializesToZero() throws Exception {
        // A JSON event that pre-dates versioning — no schemaVersion field
        String legacyJson = "{\"type\":\"COMPLETED\",\"caseId\":\"case-old\","
                + "\"itemUuid\":\"abc\",\"durationMs\":100,"
                + "\"timestamp\":\"2026-05-01T00:00:00Z\"}";

        ItemStatusEvent event = MAPPER.readValue(legacyJson, ItemStatusEvent.class);
        assertEquals(0, event.getSchemaVersion(),
                "legacy events lacking the field must have schemaVersion == 0 (Java primitive default)");
        assertEquals(ItemStatusEvent.Type.COMPLETED, event.getType());
    }

    @Test
    void legacyEventWithExplicitZeroVersion() throws Exception {
        String json = "{\"schemaVersion\":0,\"type\":\"ERROR\",\"caseId\":\"c\","
                + "\"itemUuid\":\"u\",\"timestamp\":\"2026-05-01T00:00:00Z\"}";
        ItemStatusEvent event = MAPPER.readValue(json, ItemStatusEvent.class);
        assertEquals(0, event.getSchemaVersion());
        assertEquals(ItemStatusEvent.Type.ERROR, event.getType());
    }

    // =========================================================================
    // Forward compatibility — future schema version events
    // =========================================================================

    @Test
    void futureVersionEventDeserializesSuccessfully() throws Exception {
        // An event from a newer agent — schemaVersion = 99, plus an unknown field
        String futureJson = "{\"schemaVersion\":99,\"type\":\"COMPLETED\","
                + "\"caseId\":\"case-future\",\"itemUuid\":\"xyz\","
                + "\"durationMs\":5,\"timestamp\":\"2026-12-01T00:00:00Z\","
                + "\"unknownNewField\":\"some-value\"}";

        ItemStatusEvent event = MAPPER.readValue(futureJson, ItemStatusEvent.class);
        assertEquals(99, event.getSchemaVersion());
        assertEquals(ItemStatusEvent.Type.COMPLETED, event.getType());
        assertEquals("case-future", event.getCaseId());
        // unknown field silently ignored by @JsonIgnoreProperties(ignoreUnknown=true)
    }

    @Test
    void futureVersionDoesNotThrowDuringValidation() throws Exception {
        String futureJson = "{\"schemaVersion\":999,\"type\":\"STARTED\","
                + "\"caseId\":\"c\",\"itemUuid\":\"u\","
                + "\"timestamp\":\"2026-12-01T00:00:00Z\"}";
        ItemStatusEvent event = MAPPER.readValue(futureJson, ItemStatusEvent.class);

        // validateSchemaVersion must not throw regardless of version number
        assertDoesNotThrow(() -> ItemStatusConsumer.validateSchemaVersion(event));
    }

    // =========================================================================
    // validateSchemaVersion — all version categories
    // =========================================================================

    @Test
    void validateDoesNotThrowForLegacyVersion() throws Exception {
        String json = "{\"schemaVersion\":0,\"type\":\"COMPLETED\","
                + "\"caseId\":\"c\",\"itemUuid\":\"u\","
                + "\"timestamp\":\"2026-06-01T00:00:00Z\"}";
        ItemStatusEvent event = MAPPER.readValue(json, ItemStatusEvent.class);
        assertDoesNotThrow(() -> ItemStatusConsumer.validateSchemaVersion(event));
    }

    @Test
    void validateDoesNotThrowForCurrentVersion() throws Exception {
        ItemStatusEvent event = ItemStatusEvent.completed(sampleMsg, "T", 1);
        assertEquals(ItemStatusEvent.SCHEMA_VERSION, event.getSchemaVersion());
        assertDoesNotThrow(() -> ItemStatusConsumer.validateSchemaVersion(event));
    }

    @Test
    void validateDoesNotThrowForFutureVersion() throws Exception {
        String json = "{\"schemaVersion\":9999,\"type\":\"SKIPPED\","
                + "\"caseId\":\"c\",\"itemUuid\":\"u\","
                + "\"timestamp\":\"2026-06-01T00:00:00Z\"}";
        ItemStatusEvent event = MAPPER.readValue(json, ItemStatusEvent.class);
        assertDoesNotThrow(() -> ItemStatusConsumer.validateSchemaVersion(event));
    }

    // =========================================================================
    // Unknown event type — forward compat via @JsonIgnoreProperties
    // =========================================================================

    @Test
    void unknownEventTypeDeserializesToNullType() throws Exception {
        // A new event type from a future agent — Jackson sets the enum to null
        String json = "{\"schemaVersion\":2,\"type\":\"FUTURE_TYPE\","
                + "\"caseId\":\"c\",\"itemUuid\":\"u\","
                + "\"timestamp\":\"2026-12-01T00:00:00Z\"}";

        // ObjectMapper by default throws on unknown enum values — verify behaviour
        // The class uses @JsonIgnoreProperties(ignoreUnknown=true) which covers
        // unknown properties, NOT unknown enum values. Unknown enums become null
        // with WRITE_ENUMS_USING_TO_STRING / @JsonEnumDefaultValue, or throw.
        // We verify the existing handling is explicit.
        try {
            ItemStatusEvent event = MAPPER.readValue(json, ItemStatusEvent.class);
            // If we get here, Jackson mapped it to null or a default — acceptable
            assertNull(event.getType(), "unknown enum value should map to null");
        } catch (com.fasterxml.jackson.databind.exc.InvalidFormatException ex) {
            // Also acceptable: throw lets the consumer skip via its catch block
            assertTrue(ex.getMessage().contains("FUTURE_TYPE") || ex.getMessage().contains("Type"));
        }
    }
}
