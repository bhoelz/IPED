package iped.distributed.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Read-only view of a single dead-letter-queue record returned by
 * {@link DlqManager#list}.
 *
 * <p>An entry carries enough information for an operator to understand why the item
 * failed and to decide whether to requeue or discard it:
 * <ul>
 *   <li>{@link #dlqTopic}, {@link #partition}, {@link #offset} — the DLQ coordinates
 *       required by {@link DlqManager#requeue} / {@link DlqManager#discard}.</li>
 *   <li>{@link #itemUuid}, {@link #path} — human-readable identity of the failed item.</li>
 *   <li>{@link #pipelineStage}, {@link #attempt} — where in the pipeline the item failed
 *       and how many times it was retried before landing here.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DlqEntry {

    /** The DLQ Kafka topic this entry was read from. */
    private String dlqTopic;

    /** Kafka partition within the DLQ topic. */
    private int partition;

    /** Kafka offset of this message within the partition. */
    private long offset;

    /** Stable item UUID assigned at pipeline entry. */
    private String itemUuid;

    /** Evidence path — human-readable location within the datasource. */
    private String path;

    /** Pipeline stage at which the item failed (0 = raw reader stage). */
    private int pipelineStage;

    /** Number of processing attempts made before the item was moved to the DLQ. */
    private int attempt;

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /**
     * Creates a {@code DlqEntry} from Kafka record coordinates and the message payload.
     */
    public static DlqEntry from(String dlqTopic, int partition, long offset,
                                 KafkaItemMessage msg) {
        DlqEntry e = new DlqEntry();
        e.dlqTopic      = dlqTopic;
        e.partition     = partition;
        e.offset        = offset;
        e.itemUuid      = msg.getItemUuid();
        e.path          = msg.getPath();
        e.pipelineStage = msg.getPipelineStage();
        e.attempt       = msg.getAttempt();
        return e;
    }

    // -----------------------------------------------------------------------
    // Topic name utilities
    // -----------------------------------------------------------------------

    /**
     * Derives the original stage topic name from a DLQ topic name by stripping the
     * DLQ suffix.  The original topic is where this item should be republished on
     * a requeue operation.
     *
     * @param dlqTopic the DLQ topic name, e.g. {@code iped.case1.stage.2.dlq}
     * @param suffix   the configured DLQ suffix, e.g. {@code .dlq}
     * @return the original stage topic, e.g. {@code iped.case1.stage.2}
     * @throws IllegalArgumentException if {@code dlqTopic} does not end with {@code suffix}
     */
    public static String originalTopic(String dlqTopic, String suffix) {
        if (dlqTopic == null || suffix == null || !dlqTopic.endsWith(suffix)) {
            throw new IllegalArgumentException(
                    "Topic '" + dlqTopic + "' does not end with DLQ suffix '" + suffix + "'");
        }
        return dlqTopic.substring(0, dlqTopic.length() - suffix.length());
    }

    // -----------------------------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------------------------

    public String getDlqTopic()      { return dlqTopic; }
    public void   setDlqTopic(String v) { dlqTopic = v; }

    public int  getPartition()       { return partition; }
    public void setPartition(int v)  { partition = v; }

    public long getOffset()          { return offset; }
    public void setOffset(long v)    { offset = v; }

    public String getItemUuid()      { return itemUuid; }
    public void   setItemUuid(String v) { itemUuid = v; }

    public String getPath()          { return path; }
    public void   setPath(String v)  { path = v; }

    public int  getPipelineStage()      { return pipelineStage; }
    public void setPipelineStage(int v) { pipelineStage = v; }

    public int  getAttempt()         { return attempt; }
    public void setAttempt(int v)    { attempt = v; }

    @Override
    public String toString() {
        return "DlqEntry{topic='" + dlqTopic + "', partition=" + partition
                + ", offset=" + offset + ", item='" + itemUuid + "', path='" + path
                + "', stage=" + pipelineStage + ", attempt=" + attempt + '}';
    }
}
