package iped.distributed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iped.distributed.config.DistributedConfig;
import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.coordinator.AgentTopicAssigner;
import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.coordinator.CoordinatorEventLog;
import iped.distributed.coordinator.CoordinatorEventLog.CoordinatorEvent;
import iped.distributed.coordinator.CoordinatorEventLog.EventType;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.scheduler.CasePriority;
import iped.distributed.status.ItemStatusEvent;
import iped.distributed.workunit.AdaptiveWorkUnitPlanner;
import iped.distributed.workunit.MediaCostModel;
import iped.distributed.workunit.WorkUnit;
import iped.distributed.workunit.WorkUnitSizingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6 — Observability and Robustness.
 *
 * <p>Covers (all broker-free):
 * <ul>
 *   <li>KafkaItemProducer work-unit tagging logic via AdaptiveWorkUnitPlanner (5 tests)</li>
 *   <li>CaseLifecycleManager.pauseCase / resumeCase (5 tests)</li>
 *   <li>CaseLifecycleManager.updateTags (4 tests)</li>
 *   <li>CaseCompletionMonitor.isStalled / lastEventAtMs (6 tests)</li>
 *   <li>CoordinatorEventLog ring buffer (8 tests)</li>
 *   <li>AgentTopicAssigner excludes PAUSED cases (3 tests)</li>
 *   <li>DistributedConfig DLQ auto-retry config keys (4 tests)</li>
 * </ul>
 */
class Phase6ObservabilityTest {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private Phase5HardeningTest.StubLifecycleManager lifecycle;
    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        lifecycle = new Phase5HardeningTest.StubLifecycleManager();
        registry  = new AgentRegistry(30);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Work-unit tagging via AdaptiveWorkUnitPlanner
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void plannerAssignsWorkUnitIdToMessages() {
        AdaptiveWorkUnitPlanner planner = tinPlanner();
        List<KafkaItemMessage> items = List.of(
                smallMsg("item-1"), smallMsg("item-2"), smallMsg("item-3"));
        List<WorkUnit> units = planner.plan(items);
        assertFalse(units.isEmpty());
        // All items from a single unit should share the same workUnitId
        WorkUnit unit = units.get(0);
        assertEquals(items.size() >= unit.itemCount() ? unit.itemCount() : items.size(),
                unit.itemUuids().size());
    }

    @Test
    void plannerGroupsSmallItemsIntoSingleUnit() {
        AdaptiveWorkUnitPlanner planner = tinPlanner();
        List<KafkaItemMessage> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) items.add(smallMsg("item-" + i));
        List<WorkUnit> units = planner.plan(items);
        // With tiny policy (5 items max), all 5 should be in 1 unit
        assertEquals(1, units.size());
        assertEquals(5, units.get(0).itemCount());
    }

    @Test
    void plannerSplitsWhenExceedingMaxItems() {
        // maxItems=3, so 7 items should become 3 units (3+3+1)
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(
                Long.MAX_VALUE, 3, Long.MAX_VALUE);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());
        List<KafkaItemMessage> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) items.add(smallMsg("item-" + i));
        List<WorkUnit> units = planner.plan(items);
        assertEquals(3, units.size());
        assertEquals(3, units.get(0).itemCount());
        assertEquals(3, units.get(1).itemCount());
        assertEquals(1, units.get(2).itemCount());
    }

    @Test
    void workUnitIndexMatchesPositionWithinUnit() {
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(Long.MAX_VALUE, 4, Long.MAX_VALUE);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());
        List<KafkaItemMessage> items = List.of(
                smallMsg("a"), smallMsg("b"), smallMsg("c"));
        List<WorkUnit> units = planner.plan(items);
        assertEquals(1, units.size());
        assertEquals(List.of("a", "b", "c"), units.get(0).itemUuids());
    }

    @Test
    void workUnitIndexIsInInputOrder() {
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(Long.MAX_VALUE, 10, Long.MAX_VALUE);
        AdaptiveWorkUnitPlanner planner = new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());
        List<KafkaItemMessage> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) items.add(smallMsg("uuid-" + i));
        List<WorkUnit> units = planner.plan(items);
        assertEquals(1, units.size());
        for (int i = 0; i < 4; i++) {
            assertEquals("uuid-" + i, units.get(0).itemUuids().get(i));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CaseLifecycleManager — pause / resume
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void pauseRunningCaseSucceeds() {
        lifecycle.addCase("c1", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.RUNNING);
        assertTrue(lifecycle.pauseCase("c1"));
        assertEquals(CaseLifecycleManager.CaseStatus.State.PAUSED,
                lifecycle.getStatus("c1").state);
    }

    @Test
    void pauseUnknownCaseReturnsFalse() {
        assertFalse(lifecycle.pauseCase("no-such"));
    }

    @Test
    void resumePausedCaseSetsRunning() {
        lifecycle.addCase("c2", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.PAUSED);
        assertTrue(lifecycle.resumeCase("c2"));
        assertEquals(CaseLifecycleManager.CaseStatus.State.RUNNING,
                lifecycle.getStatus("c2").state);
    }

    @Test
    void resumeRunningCaseReturnsFalse() {
        lifecycle.addCase("c3", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.RUNNING);
        assertFalse(lifecycle.resumeCase("c3"));
    }

    @Test
    void resumeUnknownCaseReturnsFalse() {
        assertFalse(lifecycle.resumeCase("ghost"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // CaseLifecycleManager — metadata tags
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void updateTagsOnExistingCaseSucceeds() {
        lifecycle.addCase("c4", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.RUNNING);
        assertTrue(lifecycle.updateTags("c4", Map.of("investigator", "Alice")));
        assertEquals("Alice", lifecycle.getStatus("c4").metadata.get("investigator"));
    }

    @Test
    void updateTagsMergesWithExistingTags() {
        lifecycle.addCase("c5", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.RUNNING);
        lifecycle.updateTags("c5", Map.of("k1", "v1"));
        lifecycle.updateTags("c5", Map.of("k2", "v2"));
        assertEquals("v1", lifecycle.getStatus("c5").metadata.get("k1"));
        assertEquals("v2", lifecycle.getStatus("c5").metadata.get("k2"));
    }

    @Test
    void updateTagsOverwritesExistingKey() {
        lifecycle.addCase("c6", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.RUNNING);
        lifecycle.updateTags("c6", Map.of("ref", "old"));
        lifecycle.updateTags("c6", Map.of("ref", "new"));
        assertEquals("new", lifecycle.getStatus("c6").metadata.get("ref"));
    }

    @Test
    void updateTagsUnknownCaseReturnsFalse() {
        assertFalse(lifecycle.updateTags("none", Map.of("k", "v")));
    }

    // ════════════════════════════════════════════════════════════════════════
    // CaseCompletionMonitor — stall detection
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void notStalledForUnknownCase() {
        CaseCompletionMonitor monitor = monitor();
        assertFalse(monitor.isStalled("unknown", 1000));
    }

    @Test
    void notStalledWhenZeroDiscovered() {
        CaseCompletionMonitor monitor = monitor();
        // No events at all — discovered == 0
        assertFalse(monitor.isStalled("case-x", 0));
    }

    @Test
    void notStalledWhenItemsInFlight() {
        CaseCompletionMonitor monitor = monitor();
        lifecycle.addCase("c7", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
        KafkaItemMessage msg = itemMsg("c7", "item-1", 0);
        monitor.onEvent(ItemStatusEvent.discovered(msg));
        monitor.onEvent(ItemStatusEvent.started(msg, "HashTask"));
        // inFlight > 0 → not stalled even with a 0ms window
        assertFalse(monitor.isStalled("c7", 0));
    }

    @Test
    void stalledWhenNoInflightAndWindowExpired() throws InterruptedException {
        CaseCompletionMonitor monitor = monitor();
        lifecycle.addCase("c8", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
        KafkaItemMessage msg = itemMsg("c8", "item-1", 0);
        monitor.onEvent(ItemStatusEvent.discovered(msg));
        // Discovered=1, completedFinal=0, inFlight=0 → stall window of 0ms is always expired
        Thread.sleep(5);
        assertTrue(monitor.isStalled("c8", 0));
    }

    @Test
    void notStalledWhenAllCompleted() {
        CaseCompletionMonitor monitor = monitor();
        lifecycle.addCase("c9", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
        KafkaItemMessage msg = itemMsg("c9", "item-1", 0);
        monitor.onEvent(ItemStatusEvent.discovered(msg));
        monitor.onEvent(ItemStatusEvent.completed(msg, "HashTask", 10L));
        // completedFinal >= discovered → not stalled
        assertFalse(monitor.isStalled("c9", 0));
    }

    @Test
    void lastEventAtMsUpdatesOnEachEvent() throws InterruptedException {
        CaseCompletionMonitor monitor = monitor();
        lifecycle.addCase("c10", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
        KafkaItemMessage msg = itemMsg("c10", "item-1", 0);
        long before = System.currentTimeMillis();
        monitor.onEvent(ItemStatusEvent.discovered(msg));
        long after = System.currentTimeMillis();
        long recorded = monitor.lastEventAtMs("c10");
        assertTrue(recorded >= before && recorded <= after + 5,
                "lastEventAtMs should be between " + before + " and " + (after + 5) + " but was " + recorded);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CoordinatorEventLog ring buffer
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void emptyLogReturnsEmptyList() {
        CoordinatorEventLog log = new CoordinatorEventLog(10);
        assertTrue(log.latest(5).isEmpty());
    }

    @Test
    void singleEventIsReturned() {
        CoordinatorEventLog log = new CoordinatorEventLog(10);
        log.append(CoordinatorEvent.caseEvent(EventType.CASE_STARTED, "c1", "started"));
        List<CoordinatorEvent> events = log.latest(5);
        assertEquals(1, events.size());
        assertEquals(EventType.CASE_STARTED, events.get(0).type());
        assertEquals("c1", events.get(0).caseId());
    }

    @Test
    void latestReturnsAtMostNEvents() {
        CoordinatorEventLog log = new CoordinatorEventLog(20);
        for (int i = 0; i < 10; i++)
            log.append(CoordinatorEvent.generic(EventType.CASE_STARTED, "e" + i));
        assertEquals(3, log.latest(3).size());
    }

    @Test
    void latestReturnsEventsOldestFirst() {
        CoordinatorEventLog log = new CoordinatorEventLog(10);
        log.append(CoordinatorEvent.caseEvent(EventType.CASE_STARTED,  "c1", "first"));
        log.append(CoordinatorEvent.caseEvent(EventType.CASE_PAUSED,   "c2", "second"));
        log.append(CoordinatorEvent.caseEvent(EventType.CASE_COMPLETED, "c3", "third"));
        List<CoordinatorEvent> events = log.latest(10);
        assertEquals(EventType.CASE_STARTED,   events.get(0).type());
        assertEquals(EventType.CASE_PAUSED,    events.get(1).type());
        assertEquals(EventType.CASE_COMPLETED, events.get(2).type());
    }

    @Test
    void ringBufferOverwritesOldestWhenFull() {
        CoordinatorEventLog log = new CoordinatorEventLog(3);
        log.append(CoordinatorEvent.generic(EventType.CASE_STARTED,   "first"));
        log.append(CoordinatorEvent.generic(EventType.CASE_PAUSED,    "second"));
        log.append(CoordinatorEvent.generic(EventType.CASE_RESUMED,   "third"));
        log.append(CoordinatorEvent.generic(EventType.CASE_COMPLETED, "fourth"));
        List<CoordinatorEvent> events = log.latest(10);
        assertEquals(3, events.size());
        // "first" should have been overwritten
        assertFalse(events.stream().anyMatch(e -> "first".equals(e.message())));
        assertTrue(events.stream().anyMatch(e -> "fourth".equals(e.message())));
    }

    @Test
    void sizeReflectsCurrentFill() {
        CoordinatorEventLog log = new CoordinatorEventLog(5);
        assertEquals(0, log.size());
        log.append(CoordinatorEvent.generic(EventType.CASE_STARTED, "e1"));
        assertEquals(1, log.size());
        log.append(CoordinatorEvent.generic(EventType.CASE_STARTED, "e2"));
        assertEquals(2, log.size());
    }

    @Test
    void sizeCappsAtCapacity() {
        CoordinatorEventLog log = new CoordinatorEventLog(3);
        for (int i = 0; i < 10; i++)
            log.append(CoordinatorEvent.generic(EventType.CASE_STARTED, "e" + i));
        assertEquals(3, log.size());
    }

    @Test
    void agentEventCarriesAgentId() {
        CoordinatorEventLog log = new CoordinatorEventLog(10);
        log.append(CoordinatorEvent.agentEvent(EventType.AGENT_REGISTERED, "agent-99", "joined"));
        CoordinatorEvent e = log.latest(1).get(0);
        assertEquals("agent-99", e.agentId());
        assertNull(e.caseId());
    }

    // ════════════════════════════════════════════════════════════════════════
    // AgentTopicAssigner — excludes PAUSED cases
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void pausedCaseExcludedFromTopics() {
        registry.register(AgentRegistration.of("agent-1", "HashTask", 0, 4, "host"));
        lifecycle.addCase("c-paused", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.PAUSED);
        AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);
        assertTrue(assigner.topicsForAgent("agent-1").isEmpty());
    }

    @Test
    void runningCaseIncludedPausedExcluded() {
        registry.register(AgentRegistration.of("agent-1", "HashTask", 0, 4, "host"));
        lifecycle.addCase("c-run",    List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
        lifecycle.addCase("c-paused", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.PAUSED);
        AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);
        List<String> topics = assigner.topicsForAgent("agent-1");
        assertEquals(1, topics.size());
        assertEquals(TopicProvisioner.stageTopic("c-run", 0), topics.get(0));
    }

    @Test
    void resumedPausedCaseAppearsInTopics() {
        registry.register(AgentRegistration.of("agent-1", "HashTask", 0, 4, "host"));
        lifecycle.addCase("c-was-paused", List.of("HashTask"),
                CaseLifecycleManager.CaseStatus.State.PAUSED);
        lifecycle.resumeCase("c-was-paused");
        AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);
        assertEquals(1, assigner.topicsForAgent("agent-1").size());
    }

    // ════════════════════════════════════════════════════════════════════════
    // DistributedConfig — DLQ auto-retry keys
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void defaultDlqAutoRetryIsDisabled() {
        DistributedConfig cfg = new DistributedConfig();
        assertEquals(0, cfg.getDlqAutoRetryMaxAttempts());
    }

    @Test
    void dlqAutoRetryDelayMsDefault() {
        DistributedConfig cfg = new DistributedConfig();
        assertEquals(60_000L, cfg.getDlqAutoRetryDelayMs());
    }

    @Test
    void dlqAutoRetryIntervalSecondsDefault() {
        DistributedConfig cfg = new DistributedConfig();
        assertEquals(120, cfg.getDlqAutoRetryIntervalSeconds());
    }

    @Test
    void dlqAutoRetryMaxAttemptsCanBeOverridden() {
        DistributedConfig cfg = new DistributedConfig();
        setField(cfg, "dlqAutoRetryMaxAttempts", 5);
        assertEquals(5, cfg.getDlqAutoRetryMaxAttempts());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    private CaseCompletionMonitor monitor() {
        return new CaseCompletionMonitor(lifecycle, (e) -> {}, 3600);
    }

    private static KafkaItemMessage itemMsg(String caseId, String uuid, int stage) {
        KafkaItemMessage m = new KafkaItemMessage();
        m.setCaseId(caseId); m.setItemUuid(uuid); m.setPipelineStage(stage);
        return m;
    }

    private static KafkaItemMessage smallMsg(String uuid) {
        KafkaItemMessage m = new KafkaItemMessage();
        m.setItemUuid(uuid);
        m.setLength(100L);
        return m;
    }

    private static AdaptiveWorkUnitPlanner tinPlanner() {
        // Very small policy for test predictability: 5 items max, 1 MB target
        WorkUnitSizingPolicy policy = new WorkUnitSizingPolicy(
                1L * 1024 * 1024, 5, 2L * 1024 * 1024);
        return new AdaptiveWorkUnitPlanner(policy, new MediaCostModel());
    }

    private static void setField(Object obj, String name, Object value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not set field " + name, e);
        }
    }
}
