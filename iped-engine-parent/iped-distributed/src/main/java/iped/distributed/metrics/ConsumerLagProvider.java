package iped.distributed.metrics;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/**
 * Queries a Kafka {@link AdminClient} for consumer-group lag on IPED pipeline stage topics.
 *
 * <h2>How lag is computed</h2>
 *
 * <p>For each consumer group whose name starts with {@code {caseId}.} (the naming convention used
 * by {@link iped.distributed.agent.TaskAgent}):
 *
 * <ol>
 *   <li>Fetch committed offsets via {@code AdminClient.listConsumerGroupOffsets}.
 *   <li>Fetch log-end offsets for the same partitions via {@code AdminClient.listOffsets} with
 *       {@code OffsetSpec.latest()}.
 *   <li>Lag = max(0, endOffset − committedOffset).
 * </ol>
 *
 * <h2>Failure handling</h2>
 *
 * <p>{@link #getLag} never throws; it returns an empty map on any error so the caller's {@code
 * /metrics} scrape succeeds even when the broker is temporarily unavailable.
 *
 * <h2>Lifecycle</h2>
 *
 * <p>This class is {@link AutoCloseable}. The owner ({@link
 * iped.distributed.coordinator.CoordinatorServer}) must call {@link #close()} on shutdown to
 * release the underlying {@code AdminClient}.
 */
@Slf4j
public class ConsumerLagProvider implements AutoCloseable {

  private final AdminClient admin;

  public ConsumerLagProvider(String bootstrapServers) {
    Properties p = new Properties();
    p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    this.admin = AdminClient.create(p);
  }

  /**
   * Returns the current consumer lag for all pipeline consumer groups that belong to {@code
   * caseId}. Returns an empty map on any error (broker unreachable, timeout, …).
   *
   * @param caseId the case identifier whose groups should be queried
   */
  public Map<DistributedMetrics.LagKey, Long> getLag(String caseId) {
    try {
      return computeLag(caseId);
    } catch (Exception ex) {
      log.warn("Consumer-lag query failed for case '{}': {}", caseId, ex.getMessage());
      return Map.of();
    }
  }

  /** Convenience method: merges lag data for all given case IDs into a single map. */
  public Map<DistributedMetrics.LagKey, Long> getLagForCases(Collection<String> caseIds) {
    Map<DistributedMetrics.LagKey, Long> result = new LinkedHashMap<>();
    for (String caseId : caseIds) {
      result.putAll(getLag(caseId));
    }
    return result;
  }

  // -------------------------------------------------------------------------

  private Map<DistributedMetrics.LagKey, Long> computeLag(String caseId) throws Exception {
    String prefix = caseId + ".";

    // Discover pipeline consumer groups for this case
    List<String> groups =
        admin.listConsumerGroups().all().get().stream()
            .map(ConsumerGroupListing::groupId)
            .filter(id -> id.startsWith(prefix))
            .collect(Collectors.toList());

    if (groups.isEmpty()) return Map.of();

    Map<DistributedMetrics.LagKey, Long> result = new LinkedHashMap<>();

    for (String group : groups) {
      Map<TopicPartition, OffsetAndMetadata> committed =
          admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get();
      if (committed.isEmpty()) continue;

      // Request log-end offsets for every committed partition
      Map<TopicPartition, OffsetSpec> specs = new HashMap<>();
      committed.keySet().forEach(tp -> specs.put(tp, OffsetSpec.latest()));

      Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
          admin.listOffsets(specs).all().get();

      for (var entry : committed.entrySet()) {
        TopicPartition tp = entry.getKey();
        long committedOffset = entry.getValue().offset();
        ListOffsetsResult.ListOffsetsResultInfo endInfo = endOffsets.get(tp);
        if (endInfo == null) continue;
        long lag = Math.max(0L, endInfo.offset() - committedOffset);
        result.put(new DistributedMetrics.LagKey(caseId, group, tp.topic(), tp.partition()), lag);
      }
    }
    return result;
  }

  @Override
  public void close() {
    try {
      admin.close();
    } catch (Exception ignored) {
    }
  }
}
