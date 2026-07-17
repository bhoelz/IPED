package iped.distributed.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Identifies a single DLQ record by its Kafka coordinates.
 *
 * <p>Used as a request body element for {@link DlqManager#requeue} and {@link DlqManager#discard}:
 * the operator obtains positions from a prior {@link DlqManager#list} call and passes them back to
 * select which items to replay or write off.
 *
 * <p>All three fields together form a unique key: the message at exactly {@code (dlqTopic,
 * partition, offset)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DlqPosition {

  /** The DLQ topic, e.g. {@code iped.case1.stage.2.dlq}. */
  private String dlqTopic;

  /** Kafka partition within the DLQ topic. */
  private int partition;

  /** Kafka offset of the target message within the partition. */
  private long offset;

  public DlqPosition() {}

  public DlqPosition(String dlqTopic, int partition, long offset) {
    this.dlqTopic = dlqTopic;
    this.partition = partition;
    this.offset = offset;
  }

  public String getDlqTopic() {
    return dlqTopic;
  }

  public void setDlqTopic(String v) {
    dlqTopic = v;
  }

  public int getPartition() {
    return partition;
  }

  public void setPartition(int v) {
    partition = v;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long v) {
    offset = v;
  }

  @Override
  public String toString() {
    return "DlqPosition{topic='"
        + dlqTopic
        + "', partition="
        + partition
        + ", offset="
        + offset
        + '}';
  }
}
