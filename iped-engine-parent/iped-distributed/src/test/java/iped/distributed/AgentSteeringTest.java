package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.coordinator.AgentTopicAssigner;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.coordinator.HeartbeatResponse;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.scheduler.CasePriority;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Phase 5: agent-side multi-case steering.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>{@link HeartbeatResponse} — construction, field access, JSON round-trip
 *   <li>{@link AgentTopicAssigner} — topic computation for various case/agent combinations
 *   <li>{@link iped.distributed.coordinator.CoordinatorClient} — heartbeat response parsing
 * </ul>
 *
 * <p>No Kafka broker or HTTP server required — all tests are broker-free.
 */
class AgentSteeringTest {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  // Re-used across tests
  private AgentRegistry registry;
  private CaseLifecycleManager lifecycle;
  private AgentTopicAssigner assigner;

  @BeforeEach
  void setUp() {
    registry = new AgentRegistry(30);
    // Use a null TopicProvisioner — we never call startCase(), only registerCase via startCase stub
    lifecycle = new StubLifecycleManager();
    assigner = new AgentTopicAssigner(registry, lifecycle);
  }

  // ── HeartbeatResponse — construction ─────────────────────────────────────

  @Test
  void heartbeatResponseDefaultConstructorHasEmptyTopics() {
    HeartbeatResponse r = new HeartbeatResponse();
    assertNotNull(r.getSubscribedTopics());
    assertTrue(r.getSubscribedTopics().isEmpty());
  }

  @Test
  void heartbeatResponseConstructorCopiesTopics() {
    List<String> topics = List.of("iped.case-1.stage.2", "iped.case-2.stage.2");
    HeartbeatResponse r = new HeartbeatResponse(topics);
    assertEquals(topics, r.getSubscribedTopics());
  }

  @Test
  void heartbeatResponseNullTopicsBecomesEmpty() {
    HeartbeatResponse r = new HeartbeatResponse(null);
    assertNotNull(r.getSubscribedTopics());
    assertTrue(r.getSubscribedTopics().isEmpty());
  }

  @Test
  void heartbeatResponseSetterNullBecomesEmpty() {
    HeartbeatResponse r = new HeartbeatResponse(List.of("topic"));
    r.setSubscribedTopics(null);
    assertNotNull(r.getSubscribedTopics());
    assertTrue(r.getSubscribedTopics().isEmpty());
  }

  // ── HeartbeatResponse — JSON round-trip ──────────────────────────────────

  @Test
  void heartbeatResponseSerializesToJson() throws Exception {
    HeartbeatResponse r = new HeartbeatResponse(List.of("iped.case-1.stage.2"));
    String json = MAPPER.writeValueAsString(r);
    assertTrue(json.contains("subscribedTopics"));
    assertTrue(json.contains("iped.case-1.stage.2"));
  }

  @Test
  void heartbeatResponseDeserializesFromJson() throws Exception {
    String json = "{\"subscribedTopics\":[\"iped.case-A.stage.3\",\"iped.case-B.stage.3\"]}";
    HeartbeatResponse r = MAPPER.readValue(json, HeartbeatResponse.class);
    assertEquals(List.of("iped.case-A.stage.3", "iped.case-B.stage.3"), r.getSubscribedTopics());
  }

  @Test
  void heartbeatResponseIgnoresUnknownFields() throws Exception {
    String json = "{\"subscribedTopics\":[],\"futureField\":\"future-value\"}";
    // Should not throw
    HeartbeatResponse r = MAPPER.readValue(json, HeartbeatResponse.class);
    assertTrue(r.getSubscribedTopics().isEmpty());
  }

  @Test
  void heartbeatResponseRoundTrip() throws Exception {
    HeartbeatResponse original =
        new HeartbeatResponse(List.of("iped.case-X.stage.1", "iped.case-Y.stage.1"));
    String json = MAPPER.writeValueAsString(original);
    HeartbeatResponse parsed = MAPPER.readValue(json, HeartbeatResponse.class);
    assertEquals(original.getSubscribedTopics(), parsed.getSubscribedTopics());
  }

  // ── AgentTopicAssigner — unknown agent ────────────────────────────────────

  @Test
  void unknownAgentReturnsEmptyTopics() {
    List<String> topics = assigner.topicsForAgent("no-such-agent");
    assertTrue(topics.isEmpty());
  }

  // ── AgentTopicAssigner — no active cases ─────────────────────────────────

  @Test
  void noActiveCasesReturnsEmptyTopics() {
    registerAgent("agent-1", "HashTask", 0);
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertTrue(topics.isEmpty());
  }

  // ── AgentTopicAssigner — single matching case ─────────────────────────────

  @Test
  void singleRunningCaseWithMatchingTaskReturnsOneTopic() {
    registerAgent("agent-1", "HashTask", 1);
    addRunningCase("case-A", List.of("IndexTask", "HashTask", "ParsingTask"));
    // HashTask is at index 1 in the ordered list
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(1, topics.size());
    assertEquals(TopicProvisioner.stageTopic("case-A", 1), topics.get(0));
  }

  @Test
  void topicReflectsCorrectStageNumber() {
    registerAgent("agent-1", "ParsingTask", 2);
    addRunningCase("case-B", List.of("HashTask", "IndexTask", "ParsingTask"));
    // ParsingTask is at index 2
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(TopicProvisioner.stageTopic("case-B", 2), topics.get(0));
  }

  // ── AgentTopicAssigner — task type not in case pipeline ──────────────────

  @Test
  void caseWithoutMatchingTaskTypeReturnsEmpty() {
    registerAgent("agent-1", "OcrTask", 0);
    addRunningCase("case-A", List.of("HashTask", "IndexTask"));
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertTrue(topics.isEmpty());
  }

  // ── AgentTopicAssigner — multiple running cases ───────────────────────────

  @Test
  void twoRunningCasesReturnTwoTopicsSorted() {
    registerAgent("agent-1", "HashTask", 0);
    addRunningCase("case-Z", List.of("HashTask", "ParsingTask"));
    addRunningCase("case-A", List.of("HashTask", "ParsingTask"));
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(2, topics.size());
    // Result must be sorted lexicographically
    assertTrue(topics.get(0).compareTo(topics.get(1)) <= 0);
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-A", 0)));
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-Z", 0)));
  }

  @Test
  void threeRunningCasesWithMixedTasksReturnsOnlyMatchingOnes() {
    registerAgent("agent-1", "IndexTask", 1);
    addRunningCase("case-1", List.of("HashTask", "IndexTask")); // stage 1 for IndexTask
    addRunningCase("case-2", List.of("HashTask", "ParsingTask")); // no IndexTask
    addRunningCase("case-3", List.of("IndexTask", "HashTask")); // stage 0 for IndexTask
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(2, topics.size());
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-1", 1)));
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-3", 0)));
    assertFalse(topics.contains(TopicProvisioner.stageTopic("case-2", 1)));
  }

  // ── AgentTopicAssigner — completed/failed cases excluded ─────────────────

  @Test
  void completedCaseIsExcluded() {
    registerAgent("agent-1", "HashTask", 0);
    addCompletedCase("case-done", List.of("HashTask"));
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertTrue(topics.isEmpty());
  }

  @Test
  void runningCaseIncludedCompletedExcluded() {
    registerAgent("agent-1", "HashTask", 0);
    addRunningCase("case-running", List.of("HashTask"));
    addCompletedCase("case-done", List.of("HashTask"));
    List<String> topics = assigner.topicsForAgent("agent-1");
    assertEquals(1, topics.size());
    assertTrue(topics.contains(TopicProvisioner.stageTopic("case-running", 0)));
  }

  // ── AgentTopicAssigner — determinism ─────────────────────────────────────

  @Test
  void resultIsDeterministicAcrossMultipleCalls() {
    registerAgent("agent-1", "HashTask", 0);
    addRunningCase("case-C", List.of("HashTask"));
    addRunningCase("case-B", List.of("HashTask"));
    addRunningCase("case-A", List.of("HashTask"));
    List<String> first = assigner.topicsForAgent("agent-1");
    List<String> second = assigner.topicsForAgent("agent-1");
    assertEquals(first, second);
  }

  // ── CoordinatorClient — heartbeat response parsing ────────────────────────

  @Test
  void heartbeatResponseEmptyJsonParsedCorrectly() throws Exception {
    String json = "{\"status\":\"ok\",\"subscribedTopics\":[]}";
    HeartbeatResponse r = MAPPER.readValue(json, HeartbeatResponse.class);
    assertTrue(r.getSubscribedTopics().isEmpty());
  }

  @Test
  void heartbeatResponseWithMultipleTopicsParsedCorrectly() throws Exception {
    String json =
        "{\"status\":\"ok\",\"subscribedTopics\":"
            + "[\"iped.case-1.stage.2\",\"iped.case-2.stage.2\"]}";
    HeartbeatResponse r = MAPPER.readValue(json, HeartbeatResponse.class);
    assertEquals(2, r.getSubscribedTopics().size());
    assertEquals("iped.case-1.stage.2", r.getSubscribedTopics().get(0));
    assertEquals("iped.case-2.stage.2", r.getSubscribedTopics().get(1));
  }

  // ── TopicProvisioner naming ────────────────────────────────────────────────

  @Test
  void stageTopicNamingConvention() {
    assertEquals("iped.case-1.stage.0", TopicProvisioner.stageTopic("case-1", 0));
    assertEquals("iped.case-1.stage.3", TopicProvisioner.stageTopic("case-1", 3));
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private void registerAgent(String agentId, String taskType, int stage) {
    AgentRegistration reg = AgentRegistration.of(agentId, taskType, stage, 4, "localhost");
    registry.register(reg);
  }

  private void addRunningCase(String caseId, List<String> tasks) {
    ((StubLifecycleManager) lifecycle)
        .addCase(caseId, tasks, CaseLifecycleManager.CaseStatus.State.RUNNING);
  }

  private void addCompletedCase(String caseId, List<String> tasks) {
    ((StubLifecycleManager) lifecycle)
        .addCase(caseId, tasks, CaseLifecycleManager.CaseStatus.State.COMPLETED);
  }

  // ── Stub CaseLifecycleManager (no TopicProvisioner, no Kafka) ────────────

  /**
   * Stub that bypasses Kafka topic provisioning for unit tests. Backed by the same
   * CaseLifecycleManager internal maps via registerCase reflection, but we instead replicate the
   * minimal API here for test clarity.
   */
  private static class StubLifecycleManager extends CaseLifecycleManager {

    private final java.util.List<CaseStatus> cases = new java.util.ArrayList<>();
    private final java.util.Map<String, java.util.Map<String, Integer>> stageMaps =
        new java.util.HashMap<>();

    StubLifecycleManager() {
      super(null); // null TopicProvisioner — never calls startCase()
    }

    void addCase(String caseId, java.util.List<String> tasks, CaseStatus.State state) {
      CaseStatus s = new CaseStatus();
      s.caseId = caseId;
      s.orderedTasks = new java.util.ArrayList<>(tasks);
      s.state = state;
      s.priority = CasePriority.NORMAL;
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
    public int getStageForTask(String caseId, String taskType) {
      java.util.Map<String, Integer> m = stageMaps.get(caseId);
      if (m == null) throw new IllegalArgumentException("Unknown case: " + caseId);
      Integer stage = m.get(taskType);
      if (stage == null)
        throw new IllegalArgumentException("Task '" + taskType + "' not in case '" + caseId + "'");
      return stage;
    }
  }
}
