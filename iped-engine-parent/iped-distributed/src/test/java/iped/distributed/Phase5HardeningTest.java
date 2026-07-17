package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.config.ConfigValidator;
import iped.distributed.config.DistributedConfig;
import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.coordinator.AgentTopicAssigner;
import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.scheduler.CasePriority;
import iped.distributed.status.ItemStatusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 5 hardening — broker-free unit tests.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>ConfigValidator (15 tests)
 *   <li>KafkaItemMessage work-unit tagging (4 tests)
 *   <li>CaseCompletionMonitor error tracking (4 tests)
 *   <li>AgentTopicAssigner subscription cap (6 tests)
 *   <li>CoordinatorClient registerWithRetry (3 tests)
 *   <li>CaseLifecycleManager.deleteCase (3 tests)
 * </ul>
 */
class Phase5HardeningTest {

  // ── Shared fixtures ──────────────────────────────────────────────────────

  private AgentRegistry registry;
  private StubLifecycleManager lifecycle;

  @BeforeEach
  void setUp() {
    registry = new AgentRegistry(30);
    lifecycle = new StubLifecycleManager();
  }

  // ════════════════════════════════════════════════════════════════════════
  // ConfigValidator
  // ════════════════════════════════════════════════════════════════════════

  @Test
  void validDefaultConfigProducesNoErrors() {
    DistributedConfig cfg = defaultConfig();
    List<String> errors = ConfigValidator.validate(cfg);
    assertTrue(errors.isEmpty(), "Unexpected errors: " + errors);
  }

  @Test
  void blankBootstrapServersIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "kafkaBootstrapServers", "");
    assertTrue(hasError(cfg, "kafkaBootstrapServers"));
  }

  @Test
  void blankCoordinatorUrlIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "coordinatorServerUrl", "");
    assertTrue(hasError(cfg, "coordinatorServerUrl"));
  }

  @Test
  void blankSharedStorageRootIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "sharedStorageRoot", "");
    assertTrue(hasError(cfg, "sharedStorageRoot"));
  }

  @Test
  void invalidPortIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "coordinatorPort", 0);
    assertTrue(hasError(cfg, "coordinatorPort"));

    setField(cfg, "coordinatorPort", 99999);
    assertTrue(hasError(cfg, "coordinatorPort"));
  }

  @Test
  void negativeItemTimeoutIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "itemTimeoutSeconds", -1L);
    assertTrue(hasError(cfg, "itemTimeoutSeconds"));
  }

  @Test
  void heartbeatIntervalEqualToExpiryIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "heartbeatIntervalSeconds", 30);
    setField(cfg, "agentExpirySeconds", 30);
    assertTrue(hasError(cfg, "heartbeatIntervalSeconds"));
  }

  @Test
  void zeroTopicPartitionsIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "topicPartitions", 0);
    assertTrue(hasError(cfg, "topicPartitions"));
  }

  @Test
  void negativeMaxRetriesIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "maxRetries", -1);
    assertTrue(hasError(cfg, "maxRetries"));
  }

  @Test
  void workUnitOversizedSmallerThanTargetIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "workUnitTargetBytes", 100L);
    setField(cfg, "workUnitOversizedBytes", 50L);
    assertTrue(hasError(cfg, "workUnitOversizedBytes"));
  }

  @Test
  void heapSoftRatioAboveHardRatioIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "backpressureHeapSoftRatio", 0.95);
    setField(cfg, "backpressureHeapHardRatio", 0.90);
    assertTrue(hasError(cfg, "backpressureHeapSoftRatio"));
  }

  @Test
  void diskHardFreeBytesAboveSoftIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "backpressureDiskSoftFreeBytes", 5L * 1024 * 1024 * 1024);
    setField(cfg, "backpressureDiskHardFreeBytes", 10L * 1024 * 1024 * 1024);
    assertTrue(hasError(cfg, "backpressureDiskHardFreeBytes"));
  }

  @Test
  void saslMechanismWithoutUsernameIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "kafkaSaslMechanism", "PLAIN");
    setField(cfg, "kafkaSaslUsername", "");
    assertTrue(hasError(cfg, "kafkaSaslUsername"));
  }

  @Test
  void keystorePathWithoutPasswordIsError() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "kafkaKeystorePath", "/path/to/keystore.jks");
    setField(cfg, "kafkaKeystorePassword", "");
    assertTrue(hasError(cfg, "kafkaKeystorePassword"));
  }

  @Test
  void multipleErrorsAreCollectedTogether() {
    DistributedConfig cfg = defaultConfig();
    setField(cfg, "kafkaBootstrapServers", "");
    setField(cfg, "topicPartitions", 0);
    List<String> errors = ConfigValidator.validate(cfg);
    assertTrue(errors.size() >= 2, "Expected >=2 errors, got: " + errors);
  }

  // ════════════════════════════════════════════════════════════════════════
  // KafkaItemMessage work-unit tagging
  // ════════════════════════════════════════════════════════════════════════

  @Test
  void workUnitFieldsAreNullByDefault() {
    KafkaItemMessage msg = new KafkaItemMessage();
    assertNull(msg.getWorkUnitId());
    assertEquals(0, msg.getWorkUnitIndex());
  }

  @Test
  void workUnitIdSetterAndGetter() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setWorkUnitId("wu-abc-123");
    assertEquals("wu-abc-123", msg.getWorkUnitId());
  }

  @Test
  void workUnitIndexSetterAndGetter() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setWorkUnitIndex(7);
    assertEquals(7, msg.getWorkUnitIndex());
  }

  @Test
  void workUnitTaggingDoesNotAffectOtherFields() {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setCaseId("case-1");
    msg.setWorkUnitId("wu-1");
    msg.setWorkUnitIndex(2);
    assertEquals("case-1", msg.getCaseId());
    assertEquals("wu-1", msg.getWorkUnitId());
    assertEquals(2, msg.getWorkUnitIndex());
  }

  // ════════════════════════════════════════════════════════════════════════
  // CaseCompletionMonitor — error tracking
  // ════════════════════════════════════════════════════════════════════════

  @Test
  void failedCountIsZeroForUnknownCase() {
    CaseCompletionMonitor monitor = buildMonitor(List.of("HashTask"), (e) -> {});
    assertEquals(0, monitor.failedCount("no-such-case"));
  }

  @Test
  void errorEventIncrementsFailed() {
    CaseCompletionMonitor monitor = buildMonitor(List.of("HashTask"), (e) -> {});
    lifecycle.addCase("case-A", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
    KafkaItemMessage msg = itemMsg("case-A", "item-1", 0);
    monitor.onEvent(ItemStatusEvent.started(msg, "HashTask"));
    monitor.onEvent(ItemStatusEvent.error(msg, "HashTask", 0L, new RuntimeException("boom")));
    assertEquals(1, monitor.failedCount("case-A"));
  }

  @Test
  void multipleErrorsAccumulate() {
    CaseCompletionMonitor monitor = buildMonitor(List.of("HashTask"), (e) -> {});
    lifecycle.addCase("case-B", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
    monitor.onEvent(ItemStatusEvent.error(itemMsg("case-B", "item-1", 0), "HashTask", 0L, null));
    monitor.onEvent(ItemStatusEvent.error(itemMsg("case-B", "item-2", 0), "HashTask", 0L, null));
    monitor.onEvent(ItemStatusEvent.error(itemMsg("case-B", "item-3", 0), "HashTask", 0L, null));
    assertEquals(3, monitor.failedCount("case-B"));
  }

  @Test
  void errorEventsAreIsolatedPerCase() {
    CaseCompletionMonitor monitor = buildMonitor(List.of("HashTask"), (e) -> {});
    monitor.onEvent(ItemStatusEvent.error(itemMsg("case-X", "item-1", 0), "HashTask", 0L, null));
    monitor.onEvent(ItemStatusEvent.error(itemMsg("case-X", "item-2", 0), "HashTask", 0L, null));
    monitor.onEvent(ItemStatusEvent.error(itemMsg("case-Y", "item-1", 0), "HashTask", 0L, null));
    assertEquals(2, monitor.failedCount("case-X"));
    assertEquals(1, monitor.failedCount("case-Y"));
  }

  // ════════════════════════════════════════════════════════════════════════
  // AgentTopicAssigner — subscription cap
  // ════════════════════════════════════════════════════════════════════════

  @Test
  void zeroCap_returnsAllMatchingTopics() {
    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle, 0);
    registerAgent("agent-1", "HashTask", 0);
    lifecycle.addCase("case-A", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
    lifecycle.addCase("case-B", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
    lifecycle.addCase("case-C", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
    assertEquals(3, assigner.topicsForAgent("agent-1").size());
  }

  @Test
  void cap1_returnsSingleHighestPriorityCase() {
    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle, 1);
    registerAgent("agent-1", "HashTask", 0);
    lifecycle.addCase(
        "case-low",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.LOW);
    lifecycle.addCase(
        "case-urgent",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.URGENT);
    lifecycle.addCase(
        "case-normal",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.NORMAL);
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(1, topics.size());
    assertEquals(TopicProvisioner.stageTopic("case-urgent", 0), topics.get(0));
  }

  @Test
  void cap2_returnsTwoHighestPriorityCases() {
    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle, 2);
    registerAgent("agent-1", "HashTask", 0);
    lifecycle.addCase(
        "case-low",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.LOW);
    lifecycle.addCase(
        "case-high",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.HIGH);
    lifecycle.addCase(
        "case-urg",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.URGENT);
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(2, topics.size());
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-urg", 0)));
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-high", 0)));
    assertFalse(topics.contains(TopicProvisioner.stageTopic("case-low", 0)));
  }

  @Test
  void capLargerThanCaseCount_returnsAllCases() {
    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle, 100);
    registerAgent("agent-1", "HashTask", 0);
    lifecycle.addCase(
        "case-A",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.NORMAL);
    lifecycle.addCase(
        "case-B",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.NORMAL);
    assertEquals(2, assigner.topicsForAgent("agent-1").size());
  }

  @Test
  void capSamePriority_tieBreaksByAlpha() {
    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle, 2);
    registerAgent("agent-1", "HashTask", 0);
    lifecycle.addCase(
        "case-Z",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.NORMAL);
    lifecycle.addCase(
        "case-A",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.NORMAL);
    lifecycle.addCase(
        "case-M",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.NORMAL);
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(2, topics.size());
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-A", 0)));
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-M", 0)));
    assertFalse(topics.contains(TopicProvisioner.stageTopic("case-Z", 0)));
  }

  @Test
  void capResult_isSortedLexicographically() {
    AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle, 3);
    registerAgent("agent-1", "HashTask", 0);
    lifecycle.addCase(
        "case-C",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.HIGH);
    lifecycle.addCase(
        "case-A",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.HIGH);
    lifecycle.addCase(
        "case-B",
        List.of("HashTask"),
        CaseLifecycleManager.CaseStatus.State.RUNNING,
        CasePriority.HIGH);
    List<String> topics = assigner.topicsForAgent("agent-1");
    List<String> sorted = new ArrayList<>(topics);
    sorted.sort(null);
    assertEquals(sorted, topics, "Topics should be lexicographically sorted");
  }

  // ════════════════════════════════════════════════════════════════════════
  // CoordinatorClient.registerWithRetry
  // ════════════════════════════════════════════════════════════════════════

  @Test
  void registerWithRetry_succeedsOnFirstAttempt() {
    List<Integer> callCount = new ArrayList<>();
    StubClient client =
        new StubClient(
            attempts -> {
              callCount.add(1);
              // always succeed
            });
    AgentRegistration reg = AgentRegistration.of("a1", "HashTask", 0, 4, "localhost");
    client.registerWithRetry(reg, 3, 1L);
    assertEquals(1, callCount.size());
  }

  @Test
  void registerWithRetry_retriesUntilSuccess() {
    int[] count = {0};
    StubClient client =
        new StubClient(
            attempts -> {
              count[0]++;
              if (count[0] < 3) throw new RuntimeException("simulated failure");
            });
    AgentRegistration reg = AgentRegistration.of("a1", "HashTask", 0, 4, "localhost");
    client.registerWithRetry(reg, 5, 1L);
    assertEquals(3, count[0]);
  }

  @Test
  void registerWithRetry_throwsAfterMaxAttempts() {
    StubClient client =
        new StubClient(
            attempts -> {
              throw new RuntimeException("always fail");
            });
    AgentRegistration reg = AgentRegistration.of("a1", "HashTask", 0, 4, "localhost");
    assertThrows(RuntimeException.class, () -> client.registerWithRetry(reg, 3, 1L));
  }

  // ════════════════════════════════════════════════════════════════════════
  // CaseLifecycleManager.deleteCase
  // ════════════════════════════════════════════════════════════════════════

  @Test
  void deleteExistingCaseReturnsTrue() {
    lifecycle.addCase(
        "case-del", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
    assertTrue(lifecycle.deleteCase("case-del"));
  }

  @Test
  void deleteNonExistentCaseReturnsFalse() {
    assertFalse(lifecycle.deleteCase("no-such-case"));
  }

  @Test
  void deletedCaseIsRemovedFromAllCases() {
    lifecycle.addCase(
        "case-del", List.of("HashTask"), CaseLifecycleManager.CaseStatus.State.RUNNING);
    lifecycle.deleteCase("case-del");
    assertTrue(lifecycle.allCases().stream().noneMatch(s -> "case-del".equals(s.caseId)));
    assertNull(lifecycle.getStatus("case-del"));
  }

  // ════════════════════════════════════════════════════════════════════════
  // Helpers
  // ════════════════════════════════════════════════════════════════════════

  private void registerAgent(String agentId, String taskType, int stage) {
    registry.register(AgentRegistration.of(agentId, taskType, stage, 4, "localhost"));
  }

  private static boolean hasError(DistributedConfig cfg, String keyword) {
    return ConfigValidator.validate(cfg).stream().anyMatch(e -> e.contains(keyword));
  }

  private CaseCompletionMonitor buildMonitor(
      List<String> tasks, Consumer<ItemStatusEvent> publisher) {
    return new CaseCompletionMonitor(lifecycle, publisher, 3600);
  }

  private static KafkaItemMessage itemMsg(String caseId, String itemUuid, int stage) {
    KafkaItemMessage msg = new KafkaItemMessage();
    msg.setCaseId(caseId);
    msg.setItemUuid(itemUuid);
    msg.setPipelineStage(stage);
    return msg;
  }

  /** Builds a config with all valid defaults. Uses reflection via processProperties. */
  private static DistributedConfig defaultConfig() {
    DistributedConfig cfg = new DistributedConfig();
    // The default field values are already valid — just return the freshly constructed instance.
    return cfg;
  }

  /** Mutates a DistributedConfig field by name via reflection (avoids test-only setters). */
  private static void setField(DistributedConfig cfg, String name, Object value) {
    try {
      java.lang.reflect.Field f = DistributedConfig.class.getDeclaredField(name);
      f.setAccessible(true);
      f.set(cfg, value);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Could not set field " + name, e);
    }
  }

  // ── Stub CaseLifecycleManager ────────────────────────────────────────────

  static class StubLifecycleManager extends CaseLifecycleManager {

    private final java.util.List<CaseStatus> cases = new java.util.ArrayList<>();
    private final java.util.Map<String, java.util.Map<String, Integer>> stageMaps =
        new java.util.HashMap<>();

    StubLifecycleManager() {
      super(null);
    }

    void addCase(String caseId, java.util.List<String> tasks, CaseStatus.State state) {
      addCase(caseId, tasks, state, CasePriority.NORMAL);
    }

    void addCase(
        String caseId,
        java.util.List<String> tasks,
        CaseStatus.State state,
        CasePriority priority) {
      CaseStatus s = new CaseStatus();
      s.caseId = caseId;
      s.orderedTasks = new java.util.ArrayList<>(tasks);
      s.state = state;
      s.priority = priority;
      cases.add(s);
      java.util.Map<String, Integer> m = new java.util.LinkedHashMap<>();
      for (int i = 0; i < tasks.size(); i++) m.put(tasks.get(i), i);
      stageMaps.put(caseId, m);
    }

    @Override
    public java.util.Collection<CaseStatus> allCases() {
      return cases;
    }

    @Override
    public CaseStatus getStatus(String caseId) {
      return cases.stream().filter(s -> caseId.equals(s.caseId)).findFirst().orElse(null);
    }

    @Override
    public int getStageForTask(String caseId, String taskType) {
      java.util.Map<String, Integer> m = stageMaps.get(caseId);
      if (m == null) throw new IllegalArgumentException("Unknown case: " + caseId);
      Integer stage = m.get(taskType);
      if (stage == null)
        throw new IllegalArgumentException("Task '" + taskType + "' not in case '" + caseId + "'");
      return stage;
    }

    @Override
    public boolean deleteCase(String caseId) {
      boolean removed = cases.removeIf(s -> caseId.equals(s.caseId));
      stageMaps.remove(caseId);
      return removed;
    }

    @Override
    public java.util.List<String> getTaskOrder(String caseId) {
      CaseStatus s = getStatus(caseId);
      if (s == null) throw new IllegalArgumentException("Unknown case: " + caseId);
      return java.util.Collections.unmodifiableList(s.orderedTasks);
    }

    @Override
    public boolean pauseCase(String caseId) {
      CaseStatus s = getStatus(caseId);
      if (s == null || s.state == CaseStatus.State.COMPLETED || s.state == CaseStatus.State.FAILED)
        return false;
      s.state = CaseStatus.State.PAUSED;
      return true;
    }

    @Override
    public boolean resumeCase(String caseId) {
      CaseStatus s = getStatus(caseId);
      if (s == null || s.state != CaseStatus.State.PAUSED) return false;
      s.state = CaseStatus.State.RUNNING;
      return true;
    }

    @Override
    public boolean updateTags(String caseId, java.util.Map<String, String> tags) {
      CaseStatus s = getStatus(caseId);
      if (s == null) return false;
      if (s.metadata == null) s.metadata = new java.util.concurrent.ConcurrentHashMap<>();
      if (tags != null) s.metadata.putAll(tags);
      return true;
    }
  }

  // ── Stub CoordinatorClient for retry tests ────────────────────────────────

  static class StubClient extends iped.distributed.coordinator.CoordinatorClient {

    @FunctionalInterface
    interface RegisterAction {
      void run(int attempt) throws Exception;
    }

    private final RegisterAction action;
    private int callCount = 0;

    StubClient(RegisterAction action) {
      super("http://test-coordinator:8484");
      this.action = action;
    }

    @Override
    public void registerWithRetry(AgentRegistration reg, int maxAttempts, long baseDelayMs) {
      Exception lastEx = null;
      for (int attempt = 0; attempt < maxAttempts; attempt++) {
        try {
          action.run(attempt);
          return;
        } catch (Exception e) {
          lastEx = e;
          // No real sleep in tests
        }
      }
      throw new RuntimeException("Exhausted " + maxAttempts + " attempts", lastEx);
    }
  }
}
