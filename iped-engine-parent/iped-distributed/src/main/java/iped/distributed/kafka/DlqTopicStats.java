package iped.distributed.kafka;

/**
 * Per-topic DLQ statistics snapshot — entry count and offset bounds.
 *
 * <p>Produced by {@link DlqManager#topicStats(String)} and served via {@code GET
 * /api/v1/dlq/{caseId}/stats}.
 *
 * @param dlqTopic the DLQ Kafka topic name
 * @param originalTopic the upstream stage topic (DLQ suffix stripped)
 * @param count total unconsumed entries (end offset − begin offset, summed across partitions)
 * @param oldestOffset smallest begin offset across all partitions (−1 if the topic is empty)
 * @param newestOffset largest end offset across all partitions (−1 if empty)
 */
public record DlqTopicStats(
    String dlqTopic, String originalTopic, long count, long oldestOffset, long newestOffset) {}
