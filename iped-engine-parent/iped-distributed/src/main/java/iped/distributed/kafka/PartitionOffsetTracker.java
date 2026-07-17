package iped.distributed.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/**
 * Tracks per-partition Kafka offsets for safe manual commit under concurrent processing.
 *
 * <p>Worker threads process items from the same partition concurrently and can finish out of order.
 * This class advances the commit watermark only through a <em>consecutive</em> run of completed
 * offsets, so no item is ever silently skipped on restart.
 *
 * <p>Semantics: at-least-once. If the agent restarts before an offset is committed, that record is
 * re-delivered and re-processed. Items must therefore be idempotent at the task level (writing the
 * same index entry twice is safe; creating a duplicate in the output topic is prevented by the
 * Kafka idempotent producer on the next stage).
 *
 * <p>Thread safety: {@link #recordPolled} and {@link #drainCommittable} are called only from the
 * single consumer thread; {@link #markDone} is called from worker threads or producer callbacks.
 * All three methods are {@code synchronized}.
 */
public class PartitionOffsetTracker {

  /** Per-partition: offset → completed? (TreeMap keeps ascending order). */
  private final Map<TopicPartition, NavigableMap<Long, Boolean>> pending = new HashMap<>();

  /**
   * Per-partition: the highest offset that has already been committed (or -1 if none). We store the
   * raw offset, not the "commit offset + 1" form, to keep the arithmetic clear inside this class.
   */
  private final Map<TopicPartition, Long> watermarks = new HashMap<>();

  /**
   * Records that the record at {@code offset} on {@code partition} was polled and will be submitted
   * for processing. Must be called on the consumer thread <em>before</em> the record is handed to
   * the worker pool, so the slot is tracked before it can complete and race with {@link
   * #drainCommittable}.
   */
  public synchronized void recordPolled(TopicPartition partition, long offset) {
    pending.computeIfAbsent(partition, k -> new TreeMap<>()).put(offset, false);
  }

  /**
   * Marks {@code offset} on {@code partition} as safely handled — either forwarded to the next
   * stage (success) or terminally disposed of via the dead-letter queue. Once all earlier offsets
   * on the partition are also done, the watermark will advance on the next {@link
   * #drainCommittable} call.
   */
  public synchronized void markDone(TopicPartition partition, long offset) {
    NavigableMap<Long, Boolean> map = pending.get(partition);
    if (map != null) {
      map.put(offset, true);
    }
  }

  /**
   * Advances the watermark for each partition through the leading run of consecutive completed
   * offsets and returns those new commit positions.
   *
   * <p>The returned map uses Kafka's "next-to-read" commit convention: the value for a given
   * partition is {@code lastCompletedOffset + 1}. Pass the result directly to {@code
   * consumer.commitAsync()} or {@code consumer.commitSync()}.
   *
   * <p>Completed entries are removed from internal state as the watermark advances. Partitions with
   * no new progress are absent from the returned map.
   */
  public synchronized Map<TopicPartition, OffsetAndMetadata> drainCommittable() {
    Map<TopicPartition, OffsetAndMetadata> result = new HashMap<>();

    for (Map.Entry<TopicPartition, NavigableMap<Long, Boolean>> e : pending.entrySet()) {
      TopicPartition tp = e.getKey();
      NavigableMap<Long, Boolean> map = e.getValue();

      long watermark = watermarks.getOrDefault(tp, -1L);
      long newWatermark = watermark;

      while (!map.isEmpty()) {
        Map.Entry<Long, Boolean> first = map.firstEntry();
        if (!first.getValue()) break; // first pending offset not finished yet — stop
        newWatermark = first.getKey();
        map.pollFirstEntry();
      }

      if (newWatermark > watermark) {
        watermarks.put(tp, newWatermark);
        result.put(tp, new OffsetAndMetadata(newWatermark + 1));
      }
    }
    return result;
  }
}
