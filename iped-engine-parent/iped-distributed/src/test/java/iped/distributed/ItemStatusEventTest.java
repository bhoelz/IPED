package iped.distributed;

import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.status.ItemStatusEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ItemStatusEventTest {

    // ---- Helper ----

    private static KafkaItemMessage sampleMsg() {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setCaseId("case-1");
        msg.setItemUuid("item-uuid-1");
        msg.setPath("/evidence/file.txt");
        msg.setPipelineStage(2);
        return msg;
    }

    // ---- Type enum ----

    @Test
    void typeEnum_hasAllExpectedValues() {
        ItemStatusEvent.Type[] types = ItemStatusEvent.Type.values();
        assertEquals(8, types.length);
    }

    @Test
    void typeEnum_discoveredExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("DISCOVERED"));
    }

    @Test
    void typeEnum_startedExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("STARTED"));
    }

    @Test
    void typeEnum_completedExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("COMPLETED"));
    }

    @Test
    void typeEnum_skippedExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("SKIPPED"));
    }

    @Test
    void typeEnum_errorExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("ERROR"));
    }

    @Test
    void typeEnum_timeoutExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("TIMEOUT"));
    }

    @Test
    void typeEnum_subitemDiscoveredExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("SUBITEM_DISCOVERED"));
    }

    @Test
    void typeEnum_caseCompletedExists() {
        assertDoesNotThrow(() -> ItemStatusEvent.Type.valueOf("CASE_COMPLETED"));
    }

    // ---- discovered() factory ----

    @Test
    void discovered_typeIsDiscovered() {
        ItemStatusEvent e = ItemStatusEvent.discovered(sampleMsg());
        assertEquals(ItemStatusEvent.Type.DISCOVERED, e.getType());
    }

    @Test
    void discovered_caseIdFromMsg() {
        ItemStatusEvent e = ItemStatusEvent.discovered(sampleMsg());
        assertEquals("case-1", e.getCaseId());
    }

    @Test
    void discovered_itemUuidFromMsg() {
        ItemStatusEvent e = ItemStatusEvent.discovered(sampleMsg());
        assertEquals("item-uuid-1", e.getItemUuid());
    }

    @Test
    void discovered_itemPathFromMsg() {
        ItemStatusEvent e = ItemStatusEvent.discovered(sampleMsg());
        assertEquals("/evidence/file.txt", e.getItemPath());
    }

    @Test
    void discovered_timestampIsRecent() {
        Instant before = Instant.now().minusSeconds(5);
        ItemStatusEvent e = ItemStatusEvent.discovered(sampleMsg());
        Instant after = Instant.now().plusSeconds(5);
        assertTrue(e.getTimestamp().isAfter(before));
        assertTrue(e.getTimestamp().isBefore(after));
    }

    // ---- started() factory ----

    @Test
    void started_typeIsStarted() {
        ItemStatusEvent e = ItemStatusEvent.started(sampleMsg(), "HashTask");
        assertEquals(ItemStatusEvent.Type.STARTED, e.getType());
    }

    @Test
    void started_taskTypeSet() {
        ItemStatusEvent e = ItemStatusEvent.started(sampleMsg(), "HashTask");
        assertEquals("HashTask", e.getTaskType());
    }

    @Test
    void started_pipelineStageFromMsg() {
        ItemStatusEvent e = ItemStatusEvent.started(sampleMsg(), "HashTask");
        assertEquals(2, e.getPipelineStage());
    }

    // ---- completed() factory ----

    @Test
    void completed_typeIsCompleted() {
        ItemStatusEvent e = ItemStatusEvent.completed(sampleMsg(), "HashTask", 150L);
        assertEquals(ItemStatusEvent.Type.COMPLETED, e.getType());
    }

    @Test
    void completed_durationMsSet() {
        ItemStatusEvent e = ItemStatusEvent.completed(sampleMsg(), "HashTask", 250L);
        assertEquals(250L, e.getDurationMs());
    }

    // ---- error() factory ----

    @Test
    void error_typeIsError() {
        ItemStatusEvent e = ItemStatusEvent.error(sampleMsg(), "HashTask", 100L, new RuntimeException("fail"));
        assertEquals(ItemStatusEvent.Type.ERROR, e.getType());
    }

    @Test
    void error_errorMessageSet() {
        ItemStatusEvent e = ItemStatusEvent.error(sampleMsg(), "HashTask", 100L, new RuntimeException("disk full"));
        assertEquals("disk full", e.getErrorMessage());
    }

    @Test
    void error_errorClassSet() {
        ItemStatusEvent e = ItemStatusEvent.error(sampleMsg(), "HashTask", 100L, new IllegalStateException("oops"));
        assertEquals(IllegalStateException.class.getName(), e.getErrorClass());
    }

    @Test
    void error_nullCause_doesNotThrow() {
        assertDoesNotThrow(() -> ItemStatusEvent.error(sampleMsg(), "HashTask", 100L, null));
    }

    @Test
    void error_nullCause_errorMessageIsUnknown() {
        ItemStatusEvent e = ItemStatusEvent.error(sampleMsg(), "HashTask", 100L, null);
        assertEquals("unknown", e.getErrorMessage());
    }

    // ---- skipped() factory ----

    @Test
    void skipped_typeIsSkipped() {
        ItemStatusEvent e = ItemStatusEvent.skipped(sampleMsg(), "IndexTask");
        assertEquals(ItemStatusEvent.Type.SKIPPED, e.getType());
    }

    @Test
    void skipped_taskTypeSet() {
        ItemStatusEvent e = ItemStatusEvent.skipped(sampleMsg(), "IndexTask");
        assertEquals("IndexTask", e.getTaskType());
    }

    // ---- subitemDiscovered() factory ----

    @Test
    void subitemDiscovered_typeIsSubitemDiscovered() {
        ItemStatusEvent e = ItemStatusEvent.subitemDiscovered(sampleMsg());
        assertEquals(ItemStatusEvent.Type.SUBITEM_DISCOVERED, e.getType());
    }

    // ---- caseCompleted() factory ----

    @Test
    void caseCompleted_typeIsCaseCompleted() {
        ItemStatusEvent e = ItemStatusEvent.caseCompleted("case-1");
        assertEquals(ItemStatusEvent.Type.CASE_COMPLETED, e.getType());
    }

    @Test
    void caseCompleted_caseIdSet() {
        ItemStatusEvent e = ItemStatusEvent.caseCompleted("case-xyz");
        assertEquals("case-xyz", e.getCaseId());
    }

    @Test
    void caseCompleted_timestampIsNotNull() {
        ItemStatusEvent e = ItemStatusEvent.caseCompleted("case-1");
        assertNotNull(e.getTimestamp());
    }

    // ---- Setters/getters ----

    @Test
    void setters_roundTrip() {
        ItemStatusEvent e = new ItemStatusEvent();
        e.setType(ItemStatusEvent.Type.STARTED);
        e.setCaseId("c1");
        e.setItemUuid("u1");
        e.setItemPath("/path");
        e.setTaskType("T1");
        e.setPipelineStage(3);
        e.setDurationMs(500L);
        e.setErrorMessage("msg");
        e.setErrorClass("java.lang.Exception");

        assertEquals(ItemStatusEvent.Type.STARTED, e.getType());
        assertEquals("c1", e.getCaseId());
        assertEquals("u1", e.getItemUuid());
        assertEquals("/path", e.getItemPath());
        assertEquals("T1", e.getTaskType());
        assertEquals(3, e.getPipelineStage());
        assertEquals(500L, e.getDurationMs());
        assertEquals("msg", e.getErrorMessage());
        assertEquals("java.lang.Exception", e.getErrorClass());
    }

    @Test
    void setTimestamp_roundTrip() {
        ItemStatusEvent e = new ItemStatusEvent();
        Instant ts = Instant.parse("2025-06-01T12:00:00Z");
        e.setTimestamp(ts);
        assertEquals(ts, e.getTimestamp());
    }
}
