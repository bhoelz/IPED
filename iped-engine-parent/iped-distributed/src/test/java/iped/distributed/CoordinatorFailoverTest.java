package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.status.ItemStatusEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Coordinator failover tests — verifies the two recovery mechanisms that let a restarted
 * coordinator rebuild its mid-case state without a broker:
 *
 * <ol>
 *   <li>{@link CaseLifecycleManager} persists case definitions to {@code cases.json} and reloads
 *       them on construction.
 *   <li>{@link CaseCompletionMonitor#recover} replays {@code iped.status} history to rebuild
 *       completion progress, suppressing re-publication of CASE_COMPLETED.
 * </ol>
 *
 * <p>Topic provisioning is mocked; no Kafka broker is required.
 */
class CoordinatorFailoverTest {

  // =========================================================================
  // CaseLifecycleManager — durable case definitions
  // =========================================================================

  /**
   * Test double for {@link TopicProvisioner} that records {@code provisionCase} calls instead of
   * talking to a broker. Avoids a Mockito dependency.
   */
  private static class RecordingProvisioner extends TopicProvisioner {
    final List<String> provisionCalls = new ArrayList<>();

    RecordingProvisioner() {
      super("localhost:0");
    }

    @Override
    public void provisionCase(String caseId, int taskCount, int partitions, short replication) {
      provisionCalls.add(caseId + ":" + taskCount + ":" + partitions + ":" + replication);
    }
  }

  private RecordingProvisioner mockProvisioner() {
    return new RecordingProvisioner();
  }

  @Test
  void caseDefinitionPersistsAndReloads(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();

    // First coordinator instance — start a case
    CaseLifecycleManager first = new CaseLifecycleManager(tp, stateDir);
    first.startCase("case-1", List.of("HashTask", "SignatureTask", "IndexTask"), 8, (short) 1);

    assertTrue(
        Files.exists(stateDir.resolve("cases.json")), "cases.json must be written on startCase");

    // Simulate crash + restart — brand new instance over the same dir
    CaseLifecycleManager restarted = new CaseLifecycleManager(tp, stateDir);

    assertEquals(1, restarted.allCases().size(), "case must survive restart");
    assertEquals(
        List.of("HashTask", "SignatureTask", "IndexTask"),
        restarted.getTaskOrder("case-1"),
        "ordered task list must be reconstructed exactly");
    assertEquals(0, restarted.getStageForTask("case-1", "HashTask"));
    assertEquals(2, restarted.getStageForTask("case-1", "IndexTask"));
  }

  @Test
  void completedStateSurvivesRestart(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();
    CaseLifecycleManager first = new CaseLifecycleManager(tp, stateDir);
    first.startCase("case-1", List.of("HashTask"), 8, (short) 1);
    first.completeCase("case-1");

    CaseLifecycleManager restarted = new CaseLifecycleManager(tp, stateDir);
    CaseLifecycleManager.CaseStatus status = restarted.getStatus("case-1");
    assertNotNull(status);
    assertEquals(
        CaseLifecycleManager.CaseStatus.State.COMPLETED,
        status.state,
        "completed state must persist across restart");
    assertNotNull(status.completedAt);
  }

  @Test
  void casePriorityPersistsAndReloads(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();
    CaseLifecycleManager first = new CaseLifecycleManager(tp, stateDir);
    first.startCase(
        "urgent-case",
        List.of("HashTask"),
        8,
        (short) 1,
        iped.distributed.scheduler.CasePriority.URGENT);
    first.startCase("normal-case", List.of("HashTask"), 8, (short) 1);
    first.setCasePriority("normal-case", iped.distributed.scheduler.CasePriority.LOW);

    CaseLifecycleManager restarted = new CaseLifecycleManager(tp, stateDir);
    assertEquals(
        iped.distributed.scheduler.CasePriority.URGENT,
        restarted.getStatus("urgent-case").priority,
        "priority set at startCase must survive restart");
    assertEquals(
        iped.distributed.scheduler.CasePriority.LOW,
        restarted.getStatus("normal-case").priority,
        "priority changed via setCasePriority must survive restart");
  }

  @Test
  void setPriorityReturnsFalseForUnknownCase(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();
    CaseLifecycleManager mgr = new CaseLifecycleManager(tp, stateDir);
    assertFalse(mgr.setCasePriority("nope", iped.distributed.scheduler.CasePriority.HIGH));
  }

  @Test
  void defaultPriorityIsNormal(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();
    CaseLifecycleManager mgr = new CaseLifecycleManager(tp, stateDir);
    mgr.startCase("c", List.of("HashTask"), 8, (short) 1);
    assertEquals(iped.distributed.scheduler.CasePriority.NORMAL, mgr.getStatus("c").priority);
  }

  @Test
  void multipleCasesAllPersist(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();
    CaseLifecycleManager first = new CaseLifecycleManager(tp, stateDir);
    first.startCase("case-a", List.of("HashTask"), 4, (short) 1);
    first.startCase("case-b", List.of("HashTask", "IndexTask"), 4, (short) 1);
    first.startCase("case-c", List.of("SignatureTask"), 4, (short) 1);

    CaseLifecycleManager restarted = new CaseLifecycleManager(tp, stateDir);
    assertEquals(3, restarted.allCases().size());
    assertEquals(2, restarted.getTaskOrder("case-b").size());
  }

  @Test
  void provisioningStillHappensOnStartCase(@TempDir Path stateDir) {
    RecordingProvisioner tp = mockProvisioner();
    CaseLifecycleManager mgr = new CaseLifecycleManager(tp, stateDir);
    mgr.startCase("case-1", List.of("HashTask", "IndexTask"), 8, (short) 2);
    // taskCount = 2, partitions = 8, replication = 2
    assertEquals(List.of("case-1:2:8:2"), tp.provisionCalls);
  }

  @Test
  void noStateDirMeansNoPersistence(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();
    // null state dir = legacy in-memory behaviour
    CaseLifecycleManager mgr = new CaseLifecycleManager(tp);
    mgr.startCase("case-1", List.of("HashTask"), 8, (short) 1);
    assertFalse(
        Files.exists(stateDir.resolve("cases.json")),
        "no file should be written when persistence is disabled");
    assertEquals(1, mgr.allCases().size(), "in-memory tracking still works");
  }

  @Test
  void emptyStateDirStartsClean(@TempDir Path stateDir) {
    TopicProvisioner tp = mockProvisioner();
    // No prior cases.json — fresh start, no error
    CaseLifecycleManager mgr = new CaseLifecycleManager(tp, stateDir);
    assertTrue(mgr.allCases().isEmpty());
  }

  // =========================================================================
  // CaseCompletionMonitor — progress recovery via status replay
  // =========================================================================

  /** Builds a lifecycle manager that knows one case with the given stage count. */
  private CaseLifecycleManager lifecycleWith(String caseId, int stages) {
    TopicProvisioner tp = mockProvisioner();
    CaseLifecycleManager mgr = new CaseLifecycleManager(tp);
    List<String> tasks = new ArrayList<>();
    for (int i = 0; i < stages; i++) tasks.add("Task" + i);
    mgr.startCase(caseId, tasks, 1, (short) 1);
    return mgr;
  }

  private static KafkaItemMessage msg(String caseId, String uuid, int stage) {
    KafkaItemMessage m = new KafkaItemMessage();
    m.setCaseId(caseId);
    m.setItemUuid(uuid);
    m.setPipelineStage(stage);
    return m;
  }

  /** The DISCOVERED event for a root item (readers emit these up front). */
  private static ItemStatusEvent discovered(String caseId, String uuid) {
    return ItemStatusEvent.discovered(msg(caseId, uuid, 0));
  }

  /**
   * The processing events for one item through a 2-stage pipeline (final stage index = 1),
   * <b>excluding</b> the DISCOVERED event (which a reader emits up front).
   */
  private static List<ItemStatusEvent> processingHistory(String caseId, String uuid) {
    List<ItemStatusEvent> h = new ArrayList<>();
    h.add(ItemStatusEvent.started(msg(caseId, uuid, 0), "Task0"));
    h.add(ItemStatusEvent.completed(msg(caseId, uuid, 0), "Task0", 10));
    h.add(ItemStatusEvent.started(msg(caseId, uuid, 1), "Task1"));
    h.add(ItemStatusEvent.completed(msg(caseId, uuid, 1), "Task1", 10));
    return h;
  }

  @Test
  void recoverRebuildsDiscoveredAndCompletedCounts() {
    CaseLifecycleManager lifecycle = lifecycleWith("case-1", 2);
    AtomicInteger published = new AtomicInteger();
    CaseCompletionMonitor monitor =
        new CaseCompletionMonitor(lifecycle, e -> published.incrementAndGet(), 3600);

    // Readers discover all three root items first, then processing proceeds:
    // two items fully processed, one still in flight at stage 0
    List<ItemStatusEvent> history = new ArrayList<>();
    history.add(discovered("case-1", "item-a"));
    history.add(discovered("case-1", "item-b"));
    history.add(discovered("case-1", "item-c"));
    history.addAll(processingHistory("case-1", "item-a"));
    history.addAll(processingHistory("case-1", "item-b"));
    history.add(ItemStatusEvent.started(msg("case-1", "item-c", 0), "Task0"));

    monitor.recover(history);

    assertEquals(3, monitor.discoveredCount("case-1"), "3 items discovered");
    assertEquals(2, monitor.completedFinalCount("case-1"), "2 reached final stage");
    assertEquals(1, monitor.inFlightCount("case-1"), "item-c still in flight");
    assertFalse(monitor.isCompleted("case-1"), "case not complete — item-c pending");
    assertEquals(
        0, published.get(), "recovery must NOT publish any events (side effects suppressed)");
  }

  @Test
  void recoverDoesNotRepublishCaseCompleted() {
    CaseLifecycleManager lifecycle = lifecycleWith("case-1", 2);
    AtomicInteger published = new AtomicInteger();
    CaseCompletionMonitor monitor =
        new CaseCompletionMonitor(lifecycle, e -> published.incrementAndGet(), 3600);

    // History where ALL discovered items completed — completion threshold reached
    List<ItemStatusEvent> history = new ArrayList<>();
    history.add(discovered("case-1", "item-a"));
    history.add(discovered("case-1", "item-b"));
    history.addAll(processingHistory("case-1", "item-a"));
    history.addAll(processingHistory("case-1", "item-b"));
    // the pre-crash coordinator already published this:
    history.add(ItemStatusEvent.caseCompleted("case-1"));

    monitor.recover(history);

    assertTrue(monitor.isCompleted("case-1"), "case recognised as completed from history");
    assertEquals(0, published.get(), "must not re-announce CASE_COMPLETED during recovery");
  }

  @Test
  void recoverThenLiveCompletionStillWorks() {
    CaseLifecycleManager lifecycle = lifecycleWith("case-1", 2);
    List<ItemStatusEvent> publishedEvents = new ArrayList<>();
    CaseCompletionMonitor monitor =
        new CaseCompletionMonitor(lifecycle, publishedEvents::add, 3600);

    // Recover: 2 discovered, 1 completed, 1 in flight (item-b at stage 1)
    List<ItemStatusEvent> history = new ArrayList<>();
    history.add(discovered("case-1", "item-a"));
    history.add(discovered("case-1", "item-b"));
    history.addAll(processingHistory("case-1", "item-a"));
    history.add(ItemStatusEvent.started(msg("case-1", "item-b", 0), "Task0"));
    history.add(ItemStatusEvent.completed(msg("case-1", "item-b", 0), "Task0", 10));
    history.add(ItemStatusEvent.started(msg("case-1", "item-b", 1), "Task1"));
    monitor.recover(history);

    assertEquals(2, monitor.discoveredCount("case-1"));
    assertEquals(1, monitor.completedFinalCount("case-1"));
    assertFalse(monitor.isCompleted("case-1"));

    // Live: item-b finishes the final stage after recovery
    monitor.onEvent(ItemStatusEvent.completed(msg("case-1", "item-b", 1), "Task1", 10));

    assertTrue(
        monitor.isCompleted("case-1"),
        "live completion after recovery must trigger CASE_COMPLETED");
    assertEquals(1, publishedEvents.size(), "exactly one CASE_COMPLETED published");
    assertEquals(ItemStatusEvent.Type.CASE_COMPLETED, publishedEvents.get(0).getType());
  }

  @Test
  void recoverIsIdempotentForCompletedCase() {
    CaseLifecycleManager lifecycle = lifecycleWith("case-1", 1);
    AtomicInteger published = new AtomicInteger();
    CaseCompletionMonitor monitor =
        new CaseCompletionMonitor(lifecycle, e -> published.incrementAndGet(), 3600);

    // Single-stage pipeline (final stage index = 0): one item, fully done
    List<ItemStatusEvent> history = new ArrayList<>();
    history.add(ItemStatusEvent.discovered(msg("case-1", "item-a", 0)));
    history.add(ItemStatusEvent.started(msg("case-1", "item-a", 0), "Task0"));
    history.add(ItemStatusEvent.completed(msg("case-1", "item-a", 0), "Task0", 5));

    monitor.recover(history);
    assertTrue(monitor.isCompleted("case-1"), "single-item single-stage case completes on replay");
    assertEquals(0, published.get(), "no publish during replay even when threshold met");

    // A second redundant live COMPLETED (re-delivery) must not double-publish
    monitor.onEvent(ItemStatusEvent.completed(msg("case-1", "item-a", 0), "Task0", 5));
    assertEquals(
        0, published.get(), "already-completed case must not re-announce on duplicate live event");
  }

  @Test
  void recoverSingleStreamingMatchesBatchRecover() {
    CaseLifecycleManager lifecycle = lifecycleWith("case-1", 2);
    CaseCompletionMonitor batch = new CaseCompletionMonitor(lifecycle, e -> {}, 3600);
    CaseCompletionMonitor streamed = new CaseCompletionMonitor(lifecycle, e -> {}, 3600);

    List<ItemStatusEvent> history = new ArrayList<>();
    history.add(discovered("case-1", "item-a"));
    history.add(discovered("case-1", "item-b"));
    history.addAll(processingHistory("case-1", "item-a"));

    batch.recover(history);
    history.forEach(streamed::recoverSingle);

    assertEquals(batch.discoveredCount("case-1"), streamed.discoveredCount("case-1"));
    assertEquals(batch.completedFinalCount("case-1"), streamed.completedFinalCount("case-1"));
    assertEquals(batch.inFlightCount("case-1"), streamed.inFlightCount("case-1"));
  }

  @Test
  void recoverEmptyHistoryLeavesCleanState() {
    CaseLifecycleManager lifecycle = lifecycleWith("case-1", 2);
    CaseCompletionMonitor monitor = new CaseCompletionMonitor(lifecycle, e -> {}, 3600);
    monitor.recover(List.of());
    assertEquals(0, monitor.discoveredCount("case-1"));
    assertFalse(monitor.isCompleted("case-1"));
  }

  @Test
  void inFlightItemRecoveredCanStillTimeout() {
    CaseLifecycleManager lifecycle = lifecycleWith("case-1", 2);
    List<ItemStatusEvent> publishedEvents = new ArrayList<>();
    // 0s timeout so any in-flight item is immediately overdue
    CaseCompletionMonitor monitor = new CaseCompletionMonitor(lifecycle, publishedEvents::add, 0);

    String uuid = UUID.randomUUID().toString();
    List<ItemStatusEvent> history = new ArrayList<>();
    history.add(ItemStatusEvent.discovered(msg("case-1", uuid, 0)));
    history.add(ItemStatusEvent.started(msg("case-1", uuid, 0), "Task0"));
    monitor.recover(history);

    assertEquals(
        1,
        monitor.inFlightCount("case-1"),
        "recovered STARTED-but-not-completed item is tracked as in flight");

    // After recovery, the periodic sweep must still detect the stuck item
    monitor.sweepTimeouts(java.time.Instant.now().plusSeconds(10));
    assertEquals(1, publishedEvents.size(), "recovered in-flight item times out");
    assertEquals(ItemStatusEvent.Type.TIMEOUT, publishedEvents.get(0).getType());
  }
}
