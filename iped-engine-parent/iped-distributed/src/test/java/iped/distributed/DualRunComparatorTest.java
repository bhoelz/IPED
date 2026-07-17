package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.dualrun.*;
import iped.distributed.status.ItemStatusEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DualRunComparatorTest {

  // ── ItemSummary construction ─────────────────────────────────────────────

  @Test
  void itemSummaryFromEvent() {
    ItemStatusEvent e = new ItemStatusEvent();
    e.setItemUuid("uuid-1");
    e.setItemPath("/evidence/foo.jpg");
    e.setMediaType("image/jpeg");
    e.setLengthBytes(12345L);
    e.setType(ItemStatusEvent.Type.DISCOVERED);

    ItemSummary s = ItemSummary.fromEvent(e);
    assertEquals("uuid-1", s.uuid());
    assertEquals("/evidence/foo.jpg", s.path());
    assertEquals("image/jpeg", s.mediaType());
    assertEquals(12345L, s.lengthBytes());
  }

  @Test
  void itemSummaryReference() {
    ItemSummary s = ItemSummary.reference("/evidence/bar.pdf", "application/pdf", 98765L);
    assertNull(s.uuid());
    assertEquals("/evidence/bar.pdf", s.path());
    assertEquals("application/pdf", s.mediaType());
    assertEquals(98765L, s.lengthBytes());
  }

  // ── INCOMPLETE when not finished ─────────────────────────────────────────

  @Test
  void incompleteWhenDistributedNotDone() {
    Map<String, ItemSummary> dist = Map.of("/a", item("a", "text/plain", 100L));
    Map<String, ItemSummary> ref = Map.of("/a", item(null, "text/plain", 100L));
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, false, true);
    assertEquals(DualRunVerdict.INCOMPLETE, r.verdict());
    assertTrue(r.summary().startsWith("INCOMPLETE"));
  }

  @Test
  void incompleteWhenReferenceNotSubmitted() {
    Map<String, ItemSummary> dist = Map.of("/a", item("a", "text/plain", 100L));
    DualRunReport r = DualRunComparator.compare("c1", dist, Map.of(), true, false);
    assertEquals(DualRunVerdict.INCOMPLETE, r.verdict());
  }

  @Test
  void incompleteWhenBothNotDone() {
    DualRunReport r = DualRunComparator.compare("c1", Map.of(), Map.of(), false, false);
    assertEquals(DualRunVerdict.INCOMPLETE, r.verdict());
  }

  // ── MATCH ────────────────────────────────────────────────────────────────

  @Test
  void matchWhenIdentical() {
    Map<String, ItemSummary> dist =
        Map.of(
            "/a", item("u1", "text/plain", 100L),
            "/b", item("u2", "image/jpeg", 200L));
    Map<String, ItemSummary> ref =
        Map.of(
            "/a", item(null, "text/plain", 100L),
            "/b", item(null, "image/jpeg", 200L));
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, true, true);
    assertEquals(DualRunVerdict.MATCH, r.verdict());
    assertEquals(2, r.distributedItemCount());
    assertEquals(2, r.referenceItemCount());
    assertTrue(r.missingFromDistributed().isEmpty());
    assertTrue(r.extraInDistributed().isEmpty());
    assertTrue(r.attributeMismatches().isEmpty());
    assertTrue(r.isPerfectMatch());
    assertTrue(r.summary().startsWith("MATCH"));
  }

  @Test
  void matchEmptyBothSides() {
    DualRunReport r = DualRunComparator.compare("c1", Map.of(), Map.of(), true, true);
    assertEquals(DualRunVerdict.MATCH, r.verdict());
    assertEquals(0, r.distributedItemCount());
    assertEquals(0, r.referenceItemCount());
  }

  // ── MISMATCH — coverage gaps ─────────────────────────────────────────────

  @Test
  void mismatchMissingFromDistributed() {
    Map<String, ItemSummary> dist = Map.of("/a", item("u1", "text/plain", 100L));
    Map<String, ItemSummary> ref =
        Map.of(
            "/a", item(null, "text/plain", 100L),
            "/b", item(null, "text/html", 50L)); // only in reference
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, true, true);
    assertEquals(DualRunVerdict.MISMATCH, r.verdict());
    assertEquals(1, r.totalMissing());
    assertEquals(List.of("/b"), r.missingFromDistributed());
    assertEquals(0, r.totalExtra());
    assertTrue(r.summary().contains("missing=1"));
  }

  @Test
  void mismatchExtraInDistributed() {
    Map<String, ItemSummary> dist =
        Map.of(
            "/a", item("u1", "text/plain", 100L),
            "/extra", item("u2", "application/zip", 500L));
    Map<String, ItemSummary> ref = Map.of("/a", item(null, "text/plain", 100L));
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, true, true);
    assertEquals(DualRunVerdict.MISMATCH, r.verdict());
    assertEquals(0, r.totalMissing());
    assertEquals(1, r.totalExtra());
    assertEquals(List.of("/extra"), r.extraInDistributed());
    assertTrue(r.summary().contains("extra=1"));
  }

  @Test
  void mismatchBothMissingAndExtra() {
    Map<String, ItemSummary> dist =
        Map.of(
            "/common", item("u1", "text/plain", 100L),
            "/dist-only", item("u2", "video/mp4", 1000L));
    Map<String, ItemSummary> ref =
        Map.of(
            "/common", item(null, "text/plain", 100L),
            "/ref-only", item(null, "image/png", 200L));
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, true, true);
    assertEquals(DualRunVerdict.MISMATCH, r.verdict());
    assertEquals(1, r.totalMissing());
    assertEquals(1, r.totalExtra());
  }

  // ── MISMATCH — attribute differences ────────────────────────────────────

  @Test
  void mismatchMediaTypeDiffers() {
    Map<String, ItemSummary> dist = Map.of("/f", item("u1", "image/jpeg", 100L));
    Map<String, ItemSummary> ref = Map.of("/f", item(null, "image/png", 100L));
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, true, true);
    assertEquals(DualRunVerdict.MISMATCH, r.verdict());
    assertEquals(1, r.totalAttributeMismatches());
    AttributeMismatch m = r.attributeMismatches().get(0);
    assertEquals("/f", m.path());
    assertEquals("mediaType", m.field());
    assertEquals("image/jpeg", m.distributedValue());
    assertEquals("image/png", m.referenceValue());
    assertTrue(r.summary().contains("attr-diff=1"));
  }

  @Test
  void mismatchLengthDiffers() {
    Map<String, ItemSummary> dist = Map.of("/f", item("u1", "text/plain", 100L));
    Map<String, ItemSummary> ref = Map.of("/f", item(null, "text/plain", 200L));
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, true, true);
    assertEquals(DualRunVerdict.MISMATCH, r.verdict());
    assertEquals(1, r.totalAttributeMismatches());
    AttributeMismatch m = r.attributeMismatches().get(0);
    assertEquals("lengthBytes", m.field());
  }

  @Test
  void noMismatchWhenOneAttributeIsNull() {
    // dist has mediaType, ref doesn't → not a mismatch (unknown on ref side)
    Map<String, ItemSummary> dist = Map.of("/f", item("u1", "image/jpeg", null));
    Map<String, ItemSummary> ref = Map.of("/f", item(null, null, 100L));
    DualRunReport r = DualRunComparator.compare("c1", dist, ref, true, true);
    assertEquals(DualRunVerdict.MATCH, r.verdict());
    assertTrue(r.attributeMismatches().isEmpty());
  }

  // ── DualRunSession ───────────────────────────────────────────────────────

  @Test
  void sessionAccumulatesDiscoveredEvents() {
    DualRunSession session = new DualRunSession("case-A");

    session.recordEvent(discoveredEvent("case-A", "u1", "/item1", "text/plain", 100L));
    session.recordEvent(discoveredEvent("case-A", "u2", "/item2", "image/jpeg", 200L));

    assertEquals(2, session.getDistributedItemCount());
    assertFalse(session.isDistributedComplete());
    assertFalse(session.isReferenceSubmitted());
  }

  @Test
  void sessionMarksCompleteOnCaseCompletedEvent() {
    DualRunSession session = new DualRunSession("case-B");
    assertFalse(session.isDistributedComplete());

    ItemStatusEvent done = new ItemStatusEvent();
    done.setCaseId("case-B");
    done.setType(ItemStatusEvent.Type.CASE_COMPLETED);
    session.recordEvent(done);

    assertTrue(session.isDistributedComplete());
  }

  @Test
  void sessionIgnoresNonDiscoveryEvents() {
    DualRunSession session = new DualRunSession("case-C");
    ItemStatusEvent started = new ItemStatusEvent();
    started.setCaseId("case-C");
    started.setType(ItemStatusEvent.Type.STARTED);
    started.setItemPath("/item1");
    session.recordEvent(started);

    assertEquals(0, session.getDistributedItemCount());
  }

  @Test
  void sessionReportIsIncompleteUntilBothSidesReady() {
    DualRunSession session = new DualRunSession("case-D");
    session.recordEvent(discoveredEvent("case-D", "u1", "/a", "text/plain", 10L));

    // Neither distributed complete nor reference submitted
    DualRunReport r = session.generateReport();
    assertEquals(DualRunVerdict.INCOMPLETE, r.verdict());
  }

  @Test
  void sessionFullMatchFlow() {
    DualRunSession session = new DualRunSession("case-E");
    session.recordEvent(discoveredEvent("case-E", "u1", "/a", "text/plain", 100L));
    session.recordEvent(discoveredEvent("case-E", "u2", "/b", "image/jpeg", 200L));

    // Mark distributed complete
    ItemStatusEvent done = new ItemStatusEvent();
    done.setCaseId("case-E");
    done.setType(ItemStatusEvent.Type.CASE_COMPLETED);
    session.recordEvent(done);

    // Submit matching reference
    session.setReference(
        List.of(
            ItemSummary.reference("/a", "text/plain", 100L),
            ItemSummary.reference("/b", "image/jpeg", 200L)));

    DualRunReport r = session.generateReport();
    assertEquals(DualRunVerdict.MATCH, r.verdict());
    assertTrue(r.isPerfectMatch());
  }

  // ── DualRunManager ───────────────────────────────────────────────────────

  @Test
  void managerCreatesSession() {
    DualRunManager mgr = new DualRunManager();
    assertFalse(mgr.hasSession("x"));
    mgr.startSession("x");
    assertTrue(mgr.hasSession("x"));
  }

  @Test
  void managerRoutesEventsToCorrectSession() {
    DualRunManager mgr = new DualRunManager();
    mgr.startSession("case-1");
    mgr.startSession("case-2");

    mgr.recordEvent(discoveredEvent("case-1", "u1", "/item-a", "text/plain", 10L));
    mgr.recordEvent(discoveredEvent("case-2", "u2", "/item-b", "text/plain", 20L));

    assertEquals(1, mgr.getReport("case-1").distributedItemCount());
    assertEquals(1, mgr.getReport("case-2").distributedItemCount());
  }

  @Test
  void managerReturnsNullReportForUnknownCase() {
    DualRunManager mgr = new DualRunManager();
    assertNull(mgr.getReport("unknown"));
  }

  @Test
  void managerSubmitReferenceAutoStartsSession() {
    DualRunManager mgr = new DualRunManager();
    assertFalse(mgr.hasSession("new-case"));

    // submitReference returns false when no session, caller should start then resubmit
    boolean ok =
        mgr.submitReference("new-case", List.of(ItemSummary.reference("/a", "text/plain", 10L)));
    assertFalse(ok); // no session yet
    assertFalse(mgr.hasSession("new-case"));

    // Start then submit
    mgr.startSession("new-case");
    ok = mgr.submitReference("new-case", List.of(ItemSummary.reference("/a", "text/plain", 10L)));
    assertTrue(ok);
  }

  // ── Summary text ─────────────────────────────────────────────────────────

  @Test
  void summaryTexts() {
    DualRunReport incomplete = DualRunComparator.compare("c", Map.of(), Map.of(), false, false);
    assertTrue(incomplete.summary().startsWith("INCOMPLETE"));

    DualRunReport match = DualRunComparator.compare("c", Map.of(), Map.of(), true, true);
    assertTrue(match.summary().startsWith("MATCH"));

    Map<String, ItemSummary> dist = Map.of("/a", item("u1", "text/plain", 1L));
    DualRunReport mismatch = DualRunComparator.compare("c", dist, Map.of(), true, true);
    assertTrue(mismatch.summary().startsWith("MISMATCH"));
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static ItemSummary item(String uuid, String mediaType, Long length) {
    return new ItemSummary(uuid, uuid != null ? "/" + uuid : "/ref", mediaType, length);
  }

  private static ItemStatusEvent discoveredEvent(
      String caseId, String uuid, String path, String mediaType, Long length) {
    ItemStatusEvent e = new ItemStatusEvent();
    e.setCaseId(caseId);
    e.setItemUuid(uuid);
    e.setItemPath(path);
    e.setMediaType(mediaType);
    e.setLengthBytes(length);
    e.setType(ItemStatusEvent.Type.DISCOVERED);
    return e;
  }
}
