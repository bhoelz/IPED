package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.coordinator.AgentTopicAssigner;
import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.coordinator.CaseLifecycleManager.CaseStatus;
import iped.distributed.coordinator.CoordinatorEventLog;
import iped.distributed.coordinator.CoordinatorEventLog.CoordinatorEvent;
import iped.distributed.coordinator.CoordinatorEventLog.EventType;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.scheduler.CasePriority;
import iped.distributed.status.ItemStatusEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Phase 7 — Coordinator lifecycle integration.
 *
 * <p>Tests the stateful integration of the five coordinator components that must work together to
 * drive a full case lifecycle without a Kafka broker:
 *
 * <ul>
 *   <li>{@link CaseLifecycleManager} — case state (RUNNING/PAUSED/COMPLETED) and persistence
 *   <li>{@link AgentRegistry} — agent pool (registration, expiry, capacity)
 *   <li>{@link AgentTopicAssigner} — topic assignment based on case state and agent capabilities
 *   <li>{@link CaseCompletionMonitor} — item progress tracking and case-completion detection
 *   <li>{@link CoordinatorEventLog} — ring-buffer event history
 * </ul>
 *
 * <p>Each test drives a coherent multi-step scenario and verifies that the output of one component
 * is correctly consumed by downstream components — the kind of integration bug that unit tests of
 * each component in isolation cannot catch.
 */
class CoordinatorLifecycleIntegrationTest {

  private static final String CASE_A = "case-alpha";
  private static final String CASE_B = "case-beta";
  private static final String HASH_TASK = "HashTask";
  private static final String INDEX_TASK = "IndexTask";

  private CaseLifecycleManager lifecycle;
  private AgentRegistry registry;
  private CoordinatorEventLog eventLog;
  private List<ItemStatusEvent> publishedEvents;
  private CaseCompletionMonitor monitor;

  @BeforeEach
  void setUp() {
    TopicProvisioner tp =
        new TopicProvisioner("localhost:0") {
          @Override
          public void provisionCase(String id, int n, int p, short r) {}

          @Override
          public void deprovisionCase(String id, int n) {}
        };
    lifecycle = new CaseLifecycleManager(tp);
    registry = new AgentRegistry(30);
    eventLog = new CoordinatorEventLog(100);
    publishedEvents = new ArrayList<>();
    monitor =
        new CaseCompletionMonitor(
            lifecycle,
            e -> {
              publishedEvents.add(e);
              if (e.getType() == ItemStatusEvent.Type.CASE_COMPLETED) {
                lifecycle.completeCase(e.getCaseId());
                eventLog.append(
                    CoordinatorEvent.caseEvent(
                        EventType.CASE_COMPLETED, e.getCaseId(), "completed"));
              }
            },
            3600);
  }

  // ── Scenario 1: single case, single agent, full happy-path lifecycle ──────

  @Test
  void happyPath_singleCaseSingleAgent_detectsCompletion() {
    // Start case
    lifecycle.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);
    eventLog.append(CoordinatorEvent.caseEvent(EventType.CASE_STARTED, CASE_A, "started"));

    // Register agent
    registry.register(AgentRegistration.of("agent-1", HASH_TASK, 0, 4, "host1"));
    eventLog.append(CoordinatorEvent.agentEvent(EventType.AGENT_REGISTERED, "agent-1", "joined"));

    // Coordinator assigns topics
    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(1, topics.size(), "agent must receive exactly 1 topic for 1 running case");

    // Simulate 3 items being discovered and processed
    for (int i = 0; i < 3; i++) {
      KafkaItemMessage msg = itemMsg(CASE_A, "item-" + i, 0);
      monitor.onEvent(ItemStatusEvent.discovered(msg));
    }
    for (int i = 0; i < 3; i++) {
      KafkaItemMessage msg = itemMsg(CASE_A, "item-" + i, 0);
      monitor.onEvent(ItemStatusEvent.started(msg, HASH_TASK));
      monitor.onEvent(ItemStatusEvent.completed(msg, HASH_TASK, 10L));
    }

    // All items completed → case should be detected
    assertTrue(
        monitor.isCompleted(CASE_A), "case must be completed after all items pass final stage");
    assertEquals(
        CaseStatus.State.COMPLETED,
        lifecycle.getStatus(CASE_A).state,
        "lifecycle state must transition to COMPLETED");
    assertEquals(
        1L,
        publishedEvents.stream()
            .filter(e -> e.getType() == ItemStatusEvent.Type.CASE_COMPLETED)
            .count(),
        "exactly one CASE_COMPLETED event must be published");
  }

  // ── Scenario 2: pause/resume changes topic assignments in lock-step ───────

  @Test
  void pauseResume_topicAssignmentChangesCoherently() {
    lifecycle.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);
    registry.register(AgentRegistration.of("agent-1", HASH_TASK, 0, 4, "host1"));

    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);

    // RUNNING: agent gets topics
    assertFalse(assigner.topicsForAgent("agent-1").isEmpty(), "topics before pause");

    // PAUSED: agent gets no topics
    lifecycle.pauseCase(CASE_A);
    eventLog.append(CoordinatorEvent.caseEvent(EventType.CASE_PAUSED, CASE_A, "paused"));
    assertTrue(assigner.topicsForAgent("agent-1").isEmpty(), "no topics while paused");

    // RESUMED: agent gets topics again
    lifecycle.resumeCase(CASE_A);
    eventLog.append(CoordinatorEvent.caseEvent(EventType.CASE_RESUMED, CASE_A, "resumed"));
    assertFalse(assigner.topicsForAgent("agent-1").isEmpty(), "topics after resume");

    // Event log should record the transitions
    List<CoordinatorEvent> events = eventLog.latest(10);
    assertTrue(events.stream().anyMatch(e -> e.type() == EventType.CASE_PAUSED));
    assertTrue(events.stream().anyMatch(e -> e.type() == EventType.CASE_RESUMED));
  }

  // ── Scenario 3: two cases, two specialised agents, no cross-contamination ─

  @Test
  void twoCasesTwoAgents_eachAgentOnlySeesItsCase() {
    lifecycle.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);
    lifecycle.startCase(CASE_B, List.of(INDEX_TASK), 4, (short) 1);

    registry.register(AgentRegistration.of("hash-agent", HASH_TASK, 0, 4, "h1"));
    registry.register(AgentRegistration.of("index-agent", INDEX_TASK, 0, 4, "h2"));

    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);

    List<String> hashTopics = assigner.topicsForAgent("hash-agent");
    List<String> indexTopics = assigner.topicsForAgent("index-agent");

    assertEquals(1, hashTopics.size(), "hash agent must see exactly 1 topic (case-alpha)");
    assertEquals(1, indexTopics.size(), "index agent must see exactly 1 topic (case-beta)");

    // No topic overlap
    hashTopics.forEach(
        t -> assertFalse(indexTopics.contains(t), "hash and index agent topics must not overlap"));
  }

  // ── Scenario 4: case deletion removes it from all components ─────────────

  @Test
  void deletedCase_disappearsFromTopicAssignmentsAndLifecycle() {
    lifecycle.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);
    registry.register(AgentRegistration.of("agent-1", HASH_TASK, 0, 4, "host1"));

    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);
    assertFalse(assigner.topicsForAgent("agent-1").isEmpty(), "topics before delete");

    lifecycle.deleteCase(CASE_A);
    eventLog.append(CoordinatorEvent.caseEvent(EventType.CASE_DELETED, CASE_A, "deleted"));

    // AgentTopicAssigner sees no topics (RUNNING filter finds nothing)
    assertTrue(assigner.topicsForAgent("agent-1").isEmpty(), "no topics after case deleted");

    // Lifecycle no longer tracks the case
    assertNull(lifecycle.getStatus(CASE_A), "deleted case must not appear in lifecycle");

    // Event log records the deletion
    assertTrue(
        eventLog.latest(5).stream()
            .anyMatch(e -> e.type() == EventType.CASE_DELETED && CASE_A.equals(e.caseId())));
  }

  // ── Scenario 5: priority-based topic cap (only N highest-priority cases) ──

  @Test
  void priorityCap_agentOnlyReceivesTopHighestPriorityCases() {
    lifecycle.startCase("urgent-case", List.of(HASH_TASK), 4, (short) 1, CasePriority.URGENT);
    lifecycle.startCase("normal-case", List.of(HASH_TASK), 4, (short) 1, CasePriority.NORMAL);
    lifecycle.startCase("low-case", List.of(HASH_TASK), 4, (short) 1, CasePriority.LOW);

    registry.register(AgentRegistration.of("agent-1", HASH_TASK, 0, 4, "host1"));

    // Cap at 2 — should return the 2 highest-priority (URGENT + NORMAL)
    AgentTopicAssigner capped = new AgentTopicAssigner(registry, lifecycle, 2);
    List<String> topics = capped.topicsForAgent("agent-1");

    assertEquals(2, topics.size(), "cap of 2 must return exactly 2 topics");
    // The URGENT topic must be present
    String urgentTopic = TopicProvisioner.stageTopic("urgent-case", 0);
    String normalTopic = TopicProvisioner.stageTopic("normal-case", 0);
    String lowTopic = TopicProvisioner.stageTopic("low-case", 0);
    assertTrue(topics.contains(urgentTopic), "urgent topic must be in the capped set");
    assertTrue(topics.contains(normalTopic), "normal topic must be in the capped set");
    assertFalse(topics.contains(lowTopic), "low-priority topic must be excluded by cap");
  }

  // ── Scenario 6: completion monitor + event log integration ────────────────

  @Test
  void completionEventIsLoggedAndMonitorAgreesOnState() {
    lifecycle.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);
    eventLog.append(CoordinatorEvent.caseEvent(EventType.CASE_STARTED, CASE_A, "started"));

    // Single item
    KafkaItemMessage msg = itemMsg(CASE_A, "only-item", 0);
    monitor.onEvent(ItemStatusEvent.discovered(msg));
    monitor.onEvent(ItemStatusEvent.started(msg, HASH_TASK));
    monitor.onEvent(ItemStatusEvent.completed(msg, HASH_TASK, 5L));

    // Monitor should have fired CASE_COMPLETED → our publisher appended to eventLog
    assertTrue(monitor.isCompleted(CASE_A));
    assertEquals(CaseStatus.State.COMPLETED, lifecycle.getStatus(CASE_A).state);

    // Event log must contain the CASE_COMPLETED event
    boolean completedInLog =
        eventLog.latest(10).stream()
            .anyMatch(e -> e.type() == EventType.CASE_COMPLETED && CASE_A.equals(e.caseId()));
    assertTrue(completedInLog, "CASE_COMPLETED event must be recorded in coordinator event log");
  }

  // ── Scenario 7: stall detection and event log emit ────────────────────────

  @Test
  void staleCase_isDetectedAsStalled_andEventCanBeLogged() throws InterruptedException {
    lifecycle.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);

    // Item discovered but never started → no inFlight → stall detected after window
    KafkaItemMessage msg = itemMsg(CASE_A, "stuck-item", 0);
    monitor.onEvent(ItemStatusEvent.discovered(msg));

    Thread.sleep(5); // ensure silence window > 0ms

    // Stall window of 0ms means any silence qualifies
    boolean stalled = monitor.isStalled(CASE_A, 0);
    assertTrue(stalled, "case with discovered-but-never-started item must be detected as stalled");

    // Operator logs a stall event
    eventLog.append(CoordinatorEvent.caseEvent(EventType.CASE_STALLED, CASE_A, "stall detected"));
    assertTrue(eventLog.latest(5).stream().anyMatch(e -> e.type() == EventType.CASE_STALLED));
  }

  // ── Scenario 8: agent expiry is reflected in pool view ───────────────────

  @Test
  void expiredAgent_isRemovedFromPool() throws InterruptedException {
    // Use a very short expiry (1 second) just to test the expiry logic
    AgentRegistry shortExpiry = new AgentRegistry(1);
    shortExpiry.register(AgentRegistration.of("agent-old", HASH_TASK, 0, 4, "host1"));
    assertEquals(1, shortExpiry.liveAgents().size(), "agent registered");

    Thread.sleep(1100); // let the expiry window pass

    // liveAgents() triggers eviction internally
    assertEquals(0, shortExpiry.liveAgents().size(), "expired agent must be removed from pool");
  }

  // ── Scenario 9: multi-case completion ordering ────────────────────────────

  @Test
  void twoCasesCompleteIndependently_noInterference() {
    lifecycle.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);
    lifecycle.startCase(CASE_B, List.of(HASH_TASK), 4, (short) 1);

    // Case A: 2 items
    for (int i = 0; i < 2; i++) {
      KafkaItemMessage m = itemMsg(CASE_A, "a-item-" + i, 0);
      monitor.onEvent(ItemStatusEvent.discovered(m));
    }
    // Case B: 3 items
    for (int i = 0; i < 3; i++) {
      KafkaItemMessage m = itemMsg(CASE_B, "b-item-" + i, 0);
      monitor.onEvent(ItemStatusEvent.discovered(m));
    }

    // Process case A fully
    for (int i = 0; i < 2; i++) {
      KafkaItemMessage m = itemMsg(CASE_A, "a-item-" + i, 0);
      monitor.onEvent(ItemStatusEvent.started(m, HASH_TASK));
      monitor.onEvent(ItemStatusEvent.completed(m, HASH_TASK, 5L));
    }

    assertTrue(monitor.isCompleted(CASE_A), "case A must complete after all 2 items processed");
    assertFalse(monitor.isCompleted(CASE_B), "case B must not be affected by case A completing");

    // Process case B fully
    for (int i = 0; i < 3; i++) {
      KafkaItemMessage m = itemMsg(CASE_B, "b-item-" + i, 0);
      monitor.onEvent(ItemStatusEvent.started(m, HASH_TASK));
      monitor.onEvent(ItemStatusEvent.completed(m, HASH_TASK, 5L));
    }

    assertTrue(monitor.isCompleted(CASE_B), "case B must complete after all 3 items processed");
    assertEquals(
        2L,
        publishedEvents.stream()
            .filter(e -> e.getType() == ItemStatusEvent.Type.CASE_COMPLETED)
            .count(),
        "exactly two CASE_COMPLETED events, one per case");
  }

  // ── Scenario 10: full recovery after coordinator restart ─────────────────

  @Test
  void coordinatorRestart_recoversStateAndContinuesProcessing(@TempDir Path stateDir) {
    TopicProvisioner tp =
        new TopicProvisioner("localhost:0") {
          @Override
          public void provisionCase(String id, int n, int p, short r) {}
        };

    // --- First coordinator instance ---
    CaseLifecycleManager first = new CaseLifecycleManager(tp, stateDir);
    first.startCase(CASE_A, List.of(HASH_TASK), 4, (short) 1);

    List<ItemStatusEvent> historyForRecovery = new ArrayList<>();
    CaseCompletionMonitor firstMonitor =
        new CaseCompletionMonitor(first, historyForRecovery::add, 3600);

    // 3 items discovered, 2 processed
    for (int i = 0; i < 3; i++)
      firstMonitor.onEvent(ItemStatusEvent.discovered(itemMsg(CASE_A, "item-" + i, 0)));
    firstMonitor.onEvent(ItemStatusEvent.started(itemMsg(CASE_A, "item-0", 0), HASH_TASK));
    firstMonitor.onEvent(ItemStatusEvent.completed(itemMsg(CASE_A, "item-0", 0), HASH_TASK, 5L));
    firstMonitor.onEvent(ItemStatusEvent.started(itemMsg(CASE_A, "item-1", 0), HASH_TASK));
    firstMonitor.onEvent(ItemStatusEvent.completed(itemMsg(CASE_A, "item-1", 0), HASH_TASK, 5L));
    // item-2 not yet processed — coordinator "crashes" here

    assertFalse(firstMonitor.isCompleted(CASE_A), "case not yet complete before crash");

    // --- Simulate coordinator restart ---
    CaseLifecycleManager restarted = new CaseLifecycleManager(tp, stateDir);
    assertEquals(1, restarted.allCases().size(), "case must survive coordinator restart");
    assertEquals(
        CaseStatus.State.RUNNING, restarted.getStatus(CASE_A).state, "RUNNING state must persist");

    List<ItemStatusEvent> afterRestartEvents = new ArrayList<>();
    CaseCompletionMonitor recoveredMonitor =
        new CaseCompletionMonitor(restarted, afterRestartEvents::add, 3600);

    // Build history from firstMonitor's collected events (what iped.status would replay)
    List<ItemStatusEvent> history = new ArrayList<>();
    history.add(ItemStatusEvent.discovered(itemMsg(CASE_A, "item-0", 0)));
    history.add(ItemStatusEvent.discovered(itemMsg(CASE_A, "item-1", 0)));
    history.add(ItemStatusEvent.discovered(itemMsg(CASE_A, "item-2", 0)));
    history.add(ItemStatusEvent.started(itemMsg(CASE_A, "item-0", 0), HASH_TASK));
    history.add(ItemStatusEvent.completed(itemMsg(CASE_A, "item-0", 0), HASH_TASK, 5L));
    history.add(ItemStatusEvent.started(itemMsg(CASE_A, "item-1", 0), HASH_TASK));
    history.add(ItemStatusEvent.completed(itemMsg(CASE_A, "item-1", 0), HASH_TASK, 5L));
    recoveredMonitor.recover(history);

    assertEquals(3, recoveredMonitor.discoveredCount(CASE_A), "3 items recovered");
    assertEquals(2, recoveredMonitor.completedFinalCount(CASE_A), "2 completions recovered");
    assertFalse(recoveredMonitor.isCompleted(CASE_A), "not completed — item-2 still pending");
    assertEquals(0, afterRestartEvents.size(), "no events published during recovery");

    // Now item-2 completes (live, after recovery)
    recoveredMonitor.onEvent(ItemStatusEvent.started(itemMsg(CASE_A, "item-2", 0), HASH_TASK));
    recoveredMonitor.onEvent(
        ItemStatusEvent.completed(itemMsg(CASE_A, "item-2", 0), HASH_TASK, 5L));

    assertTrue(
        recoveredMonitor.isCompleted(CASE_A),
        "case must complete after the last live item passes the final stage");
    assertEquals(
        1L,
        afterRestartEvents.stream()
            .filter(e -> e.getType() == ItemStatusEvent.Type.CASE_COMPLETED)
            .count(),
        "exactly one CASE_COMPLETED published after recovery");
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static KafkaItemMessage itemMsg(String caseId, String uuid, int stage) {
    KafkaItemMessage m = new KafkaItemMessage();
    m.setCaseId(caseId);
    m.setItemUuid(uuid);
    m.setPipelineStage(stage);
    return m;
  }
}
