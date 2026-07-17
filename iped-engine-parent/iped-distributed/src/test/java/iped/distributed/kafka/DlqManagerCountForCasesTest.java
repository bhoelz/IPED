package iped.distributed.kafka;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DlqManager#countForCases(java.util.Collection)}, the batched replacement
 * for looping over {@link DlqManager#count(String)} once per case (CRITICAL perf finding, PI-1
 * release review — CoordinatorServer /metrics scrape).
 *
 * <p>Covers:
 *
 * <ol>
 *   <li>a multi-case scenario produces the same per-case counts as the previous per-case sequential
 *       computation would have;
 *   <li>the batched implementation issues exactly one {@code listTopics}, one {@code
 *       describeTopics}, and two {@code listOffsets} calls total — not one per case (this is the
 *       actual perf fix being verified).
 * </ol>
 */
class DlqManagerCountForCasesTest {

  private static final String SUFFIX = ".dlq";

  @Test
  void givenMultipleActiveCases_whenCountForCasesCalled_thenPerCaseCountsAreCorrect()
      throws Exception {
    AdminClient admin = mock(AdminClient.class);

    // Two cases, each with one DLQ topic/partition.
    String topicA = "iped.case-a.stage.1.dlq";
    String topicB = "iped.case-b.stage.2.dlq";

    stubListTopics(admin, Set.of(topicA, topicB, "iped.case-a.stage.1" /* not a DLQ */));
    stubDescribeTopics(
        admin,
        Map.of(
            topicA, topicDescription(topicA, 1),
            topicB, topicDescription(topicB, 1)));

    TopicPartition tpA = new TopicPartition(topicA, 0);
    TopicPartition tpB = new TopicPartition(topicB, 0);

    // case-a: 10 unconsumed entries (end=15, begin=5); case-b: 0 (end==begin)
    stubListOffsets(admin, Map.of(tpA, 5L, tpB, 2L), Map.of(tpA, 15L, tpB, 2L));

    DlqManager manager = newManager(admin);

    Map<String, Long> counts = manager.countForCases(List.of("case-a", "case-b"));

    assertEquals(10L, counts.get("case-a"));
    assertEquals(0L, counts.get("case-b"));
    assertEquals(2, counts.size());
  }

  @Test
  void givenMultipleActiveCases_whenCountForCasesCalled_thenOnlyOneRoundTripSequenceIsIssued()
      throws Exception {
    AdminClient admin = mock(AdminClient.class);

    String topicA = "iped.case-a.stage.1.dlq";
    String topicB = "iped.case-b.stage.1.dlq";
    String topicC = "iped.case-c.stage.1.dlq";

    stubListTopics(admin, Set.of(topicA, topicB, topicC));
    stubDescribeTopics(
        admin,
        Map.of(
            topicA, topicDescription(topicA, 1),
            topicB, topicDescription(topicB, 1),
            topicC, topicDescription(topicC, 1)));

    TopicPartition tpA = new TopicPartition(topicA, 0);
    TopicPartition tpB = new TopicPartition(topicB, 0);
    TopicPartition tpC = new TopicPartition(topicC, 0);

    stubListOffsets(admin, Map.of(tpA, 0L, tpB, 0L, tpC, 0L), Map.of(tpA, 3L, tpB, 4L, tpC, 5L));

    DlqManager manager = newManager(admin);

    manager.countForCases(List.of("case-a", "case-b", "case-c"));

    // Exactly one round-trip sequence for 3 cases, not one per case.
    verify(admin, times(1)).listTopics(any(org.apache.kafka.clients.admin.ListTopicsOptions.class));
    verify(admin, times(1))
        .describeTopics(
            anyCollection(), any(org.apache.kafka.clients.admin.DescribeTopicsOptions.class));
    verify(admin, times(2))
        .listOffsets(anyMap(), any(org.apache.kafka.clients.admin.ListOffsetsOptions.class));
  }

  @Test
  void givenEmptyCaseList_whenCountForCasesCalled_thenNoAdminCallsAreMade() {
    AdminClient admin = mock(AdminClient.class);
    DlqManager manager = newManager(admin);

    Map<String, Long> counts = manager.countForCases(List.of());

    assertTrue(counts.isEmpty());
    verifyNoInteractions(admin);
  }

  @Test
  void givenBrokerFailure_whenCountForCasesCalled_thenZeroedResultReturnedForEveryCase()
      throws Exception {
    AdminClient admin = mock(AdminClient.class);
    when(admin.listTopics(any(org.apache.kafka.clients.admin.ListTopicsOptions.class)))
        .thenThrow(new org.apache.kafka.common.errors.TimeoutException("simulated"));

    DlqManager manager = newManager(admin);

    Map<String, Long> counts =
        assertDoesNotThrow(
            () -> manager.countForCases(List.of("case-a", "case-b")),
            "countForCases must never propagate — fail-open, like count(String)");

    assertEquals(0L, counts.get("case-a"));
    assertEquals(0L, counts.get("case-b"));
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────────

  private static DlqManager newManager(AdminClient admin) {
    return new DlqManager(SUFFIX, admin);
  }

  private static TopicDescription topicDescription(String name, int partitions) {
    Node node = new Node(0, "localhost", 9092);
    List<TopicPartitionInfo> infos = new java.util.ArrayList<>();
    for (int i = 0; i < partitions; i++) {
      infos.add(new TopicPartitionInfo(i, node, List.of(node), List.of(node)));
    }
    return new TopicDescription(name, false, infos);
  }

  private static void stubListTopics(AdminClient admin, Set<String> topicNames) {
    ListTopicsResult result = mock(ListTopicsResult.class);
    KafkaFutureImpl<Set<String>> future = new KafkaFutureImpl<>();
    future.complete(topicNames);
    when(result.names()).thenReturn(future);
    when(admin.listTopics(any(org.apache.kafka.clients.admin.ListTopicsOptions.class)))
        .thenReturn(result);
  }

  private static void stubDescribeTopics(
      AdminClient admin, Map<String, TopicDescription> descriptions) {
    DescribeTopicsResult result = mock(DescribeTopicsResult.class);
    KafkaFutureImpl<Map<String, TopicDescription>> future = new KafkaFutureImpl<>();
    future.complete(descriptions);
    when(result.allTopicNames()).thenReturn(future);
    when(admin.describeTopics(
            anyCollection(), any(org.apache.kafka.clients.admin.DescribeTopicsOptions.class)))
        .thenReturn(result);
  }

  private static void stubListOffsets(
      AdminClient admin,
      Map<TopicPartition, Long> beginOffsets,
      Map<TopicPartition, Long> endOffsets) {
    ListOffsetsResult beginResult = mock(ListOffsetsResult.class);
    ListOffsetsResult endResult = mock(ListOffsetsResult.class);

    KafkaFutureImpl<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>> beginFuture =
        new KafkaFutureImpl<>();
    beginFuture.complete(toResultInfoMap(beginOffsets));
    when(beginResult.all()).thenReturn(beginFuture);

    KafkaFutureImpl<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>> endFuture =
        new KafkaFutureImpl<>();
    endFuture.complete(toResultInfoMap(endOffsets));
    when(endResult.all()).thenReturn(endFuture);

    when(admin.listOffsets(
            argThat(
                specs ->
                    specs != null
                        && specs.values().stream()
                            .allMatch(s -> s instanceof OffsetSpec.EarliestSpec)),
            any(org.apache.kafka.clients.admin.ListOffsetsOptions.class)))
        .thenReturn(beginResult);
    when(admin.listOffsets(
            argThat(
                specs ->
                    specs != null
                        && specs.values().stream()
                            .allMatch(s -> s instanceof OffsetSpec.LatestSpec)),
            any(org.apache.kafka.clients.admin.ListOffsetsOptions.class)))
        .thenReturn(endResult);
  }

  private static Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> toResultInfoMap(
      Map<TopicPartition, Long> offsets) {
    Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> out = new java.util.HashMap<>();
    offsets.forEach(
        (tp, offset) ->
            out.put(
                tp,
                new ListOffsetsResult.ListOffsetsResultInfo(
                    offset, -1L, java.util.Optional.empty())));
    return out;
  }
}
