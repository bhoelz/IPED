package iped.distributed;

import static org.junit.jupiter.api.Assertions.*;

import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.status.ItemStatusEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseCompletionMonitorTest {

  private static final String CASE = "case-1";
  private static final List<String> TASKS = List.of("TypeDetectionTask", "HashTask", "IndexTask");

  private CaseLifecycleManager lifecycle;
  private CaseCompletionMonitor monitor;
  private List<ItemStatusEvent> published;

  @BeforeEach
  void setUp() {
    // TopicProvisioner that doesn't touch a real broker
    lifecycle =
        new CaseLifecycleManager(
            new TopicProvisioner("localhost:1") {
              @Override
              public void provisionCase(
                  String caseId, int stages, int partitions, short replication) {
                // no-op for tests
              }
            });
    lifecycle.startCase(CASE, TASKS, 1, (short) 1);

    published = new ArrayList<>();
    monitor = new CaseCompletionMonitor(lifecycle, published::add, 60);
  }

  private static KafkaItemMessage msg(String uuid, int stage) {
    KafkaItemMessage m = new KafkaItemMessage();
    m.setCaseId(CASE);
    m.setItemUuid(uuid);
    m.setPath("/img/" + uuid);
    m.setPipelineStage(stage);
    return m;
  }

  private void discoverAndFinish(String uuid) {
    monitor.onEvent(ItemStatusEvent.discovered(msg(uuid, 0)));
    monitor.onEvent(ItemStatusEvent.completed(msg(uuid, TASKS.size() - 1), "IndexTask", 5));
  }

  @Test
  void publishesCaseCompletedWhenAllItemsFinishFinalStage() {
    monitor.onEvent(ItemStatusEvent.discovered(msg("a", 0)));
    monitor.onEvent(ItemStatusEvent.discovered(msg("b", 0)));

    monitor.onEvent(ItemStatusEvent.completed(msg("a", 2), "IndexTask", 5));
    assertTrue(published.isEmpty(), "must not complete while items are pending");

    monitor.onEvent(ItemStatusEvent.completed(msg("b", 2), "IndexTask", 5));
    assertEquals(1, published.size());
    assertEquals(ItemStatusEvent.Type.CASE_COMPLETED, published.get(0).getType());
    assertEquals(CaseLifecycleManager.CaseStatus.State.COMPLETED, lifecycle.getStatus(CASE).state);
  }

  @Test
  void caseCompletedIsPublishedOnlyOnce() {
    discoverAndFinish("a");
    discoverAndFinish("b"); // second completion after counts already match

    long completions =
        published.stream().filter(e -> e.getType() == ItemStatusEvent.Type.CASE_COMPLETED).count();
    assertEquals(1, completions);
  }

  @Test
  void intermediateStageCompletionDoesNotCompleteCase() {
    monitor.onEvent(ItemStatusEvent.discovered(msg("a", 0)));
    monitor.onEvent(ItemStatusEvent.completed(msg("a", 0), "TypeDetectionTask", 5));
    monitor.onEvent(ItemStatusEvent.completed(msg("a", 1), "HashTask", 5));
    assertTrue(published.isEmpty());
  }

  @Test
  void subitemsMustAlsoCompleteBeforeCaseCompletes() {
    monitor.onEvent(ItemStatusEvent.discovered(msg("a", 0)));
    monitor.onEvent(ItemStatusEvent.subitemDiscovered(msg("a.1", 0)));

    monitor.onEvent(ItemStatusEvent.completed(msg("a", 2), "IndexTask", 5));
    assertTrue(published.isEmpty(), "sub-item still pending");

    monitor.onEvent(ItemStatusEvent.completed(msg("a.1", 2), "IndexTask", 5));
    assertEquals(1, published.size());
  }

  @Test
  void sweepPublishesTimeoutForStuckItemsExactlyOnce() {
    monitor.onEvent(ItemStatusEvent.discovered(msg("a", 0)));
    ItemStatusEvent started = ItemStatusEvent.started(msg("a", 1), "HashTask");
    started.setTimestamp(Instant.now().minusSeconds(120)); // older than 60s timeout
    monitor.onEvent(started);

    monitor.sweepTimeouts(Instant.now());
    monitor.sweepTimeouts(Instant.now()); // second sweep must not re-report

    long timeouts =
        published.stream().filter(e -> e.getType() == ItemStatusEvent.Type.TIMEOUT).count();
    assertEquals(1, timeouts);
    ItemStatusEvent t = published.get(0);
    assertEquals("a", t.getItemUuid());
    assertEquals("HashTask", t.getTaskType());
  }

  @Test
  void completedItemIsNotReportedAsTimeout() {
    monitor.onEvent(ItemStatusEvent.discovered(msg("a", 0)));
    ItemStatusEvent started = ItemStatusEvent.started(msg("a", 1), "HashTask");
    started.setTimestamp(Instant.now().minusSeconds(120));
    monitor.onEvent(started);
    monitor.onEvent(ItemStatusEvent.completed(msg("a", 1), "HashTask", 5));

    monitor.sweepTimeouts(Instant.now());
    assertTrue(published.stream().noneMatch(e -> e.getType() == ItemStatusEvent.Type.TIMEOUT));
  }
}
