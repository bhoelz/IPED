package iped.runner.distributed;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Exercises the iped.status JSON contract and the per-case aggregation without a broker. */
class DistributedStatusServiceTest {

  private static String event(String type, String caseId, int stage, String extra) {
    return """
                {"type":"%s","caseId":"%s","itemUuid":"u-1","itemPath":"/img/file%d.bin",
                 "pipelineStage":%d,"timestamp":"2026-06-11T12:00:00Z"%s}
                """
        .formatted(type, caseId, stage, stage, extra.isEmpty() ? "" : "," + extra);
  }

  @Test
  void aggregatesCaseProgressFromStatusEvents() {
    var svc = new DistributedStatusService();

    // 3 items discovered, 2 finish stage 2 (highest seen), 1 errors out
    for (int i = 0; i < 3; i++) svc.handleMessage(event("DISCOVERED", "case-1", 0, ""));
    svc.handleMessage(
        event("COMPLETED", "case-1", 1, "\"taskType\":\"HashTask\",\"durationMs\":10"));
    svc.handleMessage(
        event("COMPLETED", "case-1", 2, "\"taskType\":\"IndexTask\",\"durationMs\":12"));
    svc.handleMessage(
        event("COMPLETED", "case-1", 2, "\"taskType\":\"IndexTask\",\"durationMs\":15"));
    svc.handleMessage(
        event(
            "ERROR", "case-1", 1, "\"taskType\":\"HashTask\",\"errorMessage\":\"corrupt stream\""));

    var snapshots = svc.snapshots();
    assertEquals(1, snapshots.size());

    var snap = snapshots.get(0);
    assertEquals("dist:case-1", snap.id());
    assertEquals("running", snap.status());
    assertEquals(3, snap.itemsFound());
    assertEquals(2, snap.itemsProcessed()); // completions at the highest stage (2)
    assertTrue(snap.recentLines().stream().anyMatch(l -> l.contains("corrupt stream")));
  }

  @Test
  void caseCompletedMarksJobDone() {
    var svc = new DistributedStatusService();
    svc.handleMessage(event("DISCOVERED", "case-2", 0, ""));
    svc.handleMessage(
        "{\"type\":\"CASE_COMPLETED\",\"caseId\":\"case-2\",\"timestamp\":\"2026-06-11T12:05:00Z\"}");

    assertEquals("done", svc.snapshots().get(0).status());
  }

  @Test
  void subitemsCountTowardItemsFound() {
    var svc = new DistributedStatusService();
    svc.handleMessage(event("DISCOVERED", "case-3", 0, ""));
    svc.handleMessage(event("SUBITEM_DISCOVERED", "case-3", 0, ""));

    assertEquals(2, svc.snapshots().get(0).itemsFound());
  }

  @Test
  void ignoresMalformedAndUnknownEvents() {
    var svc = new DistributedStatusService();
    svc.handleMessage("not json at all");
    svc.handleMessage("{\"caseId\":\"x\"}"); // no type
    svc.handleMessage("{\"type\":\"DISCOVERED\"}"); // no caseId
    svc.handleMessage(
        event("DISCOVERED", "case-4", 0, "\"futureField\":\"ignored\"")); // schema evolution

    assertEquals(1, svc.snapshots().size());
    assertEquals(1, svc.snapshots().get(0).itemsFound());
  }
}
