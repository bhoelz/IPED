package iped.distributed.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * One-shot, <b>bounded</b> reader of the {@code iped.status} topic from the beginning to the
 * current log-end, used by a restarting coordinator to rebuild completion progress (see {@link
 * iped.distributed.coordinator.CaseCompletionMonitor#recover}).
 *
 * <p>Unlike {@link ItemStatusConsumer} — which is a long-lived live subscription — this reader uses
 * a fresh random consumer group, seeks to the earliest offset, and stops as soon as every partition
 * has reached the end offset captured at start time. It commits nothing, so it never interferes
 * with the live coordinator group.
 *
 * <p>Recovery is <b>best-effort</b>: if the broker is unreachable or the topic does not yet exist,
 * {@link #replayInto} logs a warning and returns 0 rather than throwing, so a coordinator can still
 * start (it simply begins with empty progress and rebuilds from live events going forward).
 */
@Slf4j
public class StatusTopicReplayer {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private final String bootstrapServers;
  private final Duration pollTimeout;

  public StatusTopicReplayer(String bootstrapServers) {
    this(bootstrapServers, Duration.ofSeconds(2));
  }

  public StatusTopicReplayer(String bootstrapServers, Duration pollTimeout) {
    this.bootstrapServers = bootstrapServers;
    this.pollTimeout = pollTimeout;
  }

  /**
   * Reads the whole {@code iped.status} topic and feeds each parsed event to {@code sink} in offset
   * order per partition.
   *
   * @param sink receiver of historical events (typically {@code monitor::recover} wrapped, or a
   *     collector); never receives unparseable records
   * @return number of events successfully delivered to the sink
   */
  public int replayInto(Consumer<ItemStatusEvent> sink) {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "iped-coordinator-recovery-" + UUID.randomUUID());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    int delivered = 0;
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
      List<PartitionInfo> partitionInfos = consumer.partitionsFor(ItemStatusProducer.STATUS_TOPIC);
      if (partitionInfos == null || partitionInfos.isEmpty()) {
        log.info(
            "Status topic '{}' has no partitions yet — nothing to replay",
            ItemStatusProducer.STATUS_TOPIC);
        return 0;
      }

      List<TopicPartition> partitions = new ArrayList<>();
      for (PartitionInfo pi : partitionInfos) {
        partitions.add(new TopicPartition(pi.topic(), pi.partition()));
      }
      consumer.assign(partitions);
      consumer.seekToBeginning(partitions);

      Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

      // Read until every partition has caught up to its captured end offset.
      while (!caughtUp(consumer, partitions, endOffsets)) {
        ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
        if (records.isEmpty()) break; // no more data within the poll window
        for (ConsumerRecord<String, String> rec : records) {
          ItemStatusEvent event = parse(rec.value());
          if (event != null) {
            sink.accept(event);
            delivered++;
          }
        }
      }
      log.info(
          "Replayed {} status event(s) from '{}' for coordinator recovery",
          delivered,
          ItemStatusProducer.STATUS_TOPIC);
    } catch (Exception e) {
      log.warn(
          "Status-topic recovery replay failed ({}); coordinator will start with "
              + "empty progress and rebuild from live events",
          e.getMessage());
    }
    return delivered;
  }

  private static boolean caughtUp(
      KafkaConsumer<String, String> consumer,
      List<TopicPartition> partitions,
      Map<TopicPartition, Long> endOffsets) {
    for (TopicPartition tp : partitions) {
      long end = endOffsets.getOrDefault(tp, 0L);
      if (end == 0) continue; // empty partition
      if (consumer.position(tp) < end) return false;
    }
    return true;
  }

  private static ItemStatusEvent parse(String json) {
    try {
      return MAPPER.readValue(json, ItemStatusEvent.class);
    } catch (Exception e) {
      return null; // skip unparseable historical records
    }
  }
}
