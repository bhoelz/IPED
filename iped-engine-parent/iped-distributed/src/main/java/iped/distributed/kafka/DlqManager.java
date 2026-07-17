package iped.distributed.kafka;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Operator tooling for IPED dead-letter queues (DLQs).
 *
 * <h2>Operations</h2>
 *
 * <dl>
 *   <dt>{@link #list}
 *   <dd>Peek at DLQ entries for a case without consuming them. Safe to call at any time.
 *   <dt>{@link #requeue}
 *   <dd>Move specific DLQ items back to their original stage topic with a fresh attempt counter so
 *       agents will retry them. Commits the DLQ offset to mark them as handled.
 *   <dt>{@link #discard}
 *   <dd>Permanently skip specific DLQ items without reprocessing. Commits the DLQ offset to mark
 *       them as handled.
 * </dl>
 *
 * <h2>DLQ topic naming</h2>
 *
 * <p>DLQ topics follow the pattern {@code {stageTopic}{dlqSuffix}}, e.g. {@code
 * iped.case1.stage.2.dlq} for the DLQ of stage&nbsp;2 in case {@code case1} with the default suffix
 * {@code .dlq}. The original stage topic is derived by stripping the suffix; see {@link
 * DlqEntry#originalTopic}.
 *
 * <h2>Consumer group</h2>
 *
 * <p>Requeue and discard operations use the consumer group {@code iped.dlq.ops.{caseId}} to commit
 * which DLQ offsets have been handled. List uses a temporary read-only consumer that does not
 * commit any offset.
 *
 * <h2>Thread safety</h2>
 *
 * <p>Each operation creates a short-lived Kafka consumer and/or producer. The only shared state is
 * the {@code AdminClient} used to discover DLQ topics; it is thread-safe.
 */
@Slf4j
public class DlqManager implements AutoCloseable {

  /** Default cap on entries returned by {@link #list} when no limit is specified. */
  public static final int DEFAULT_LIST_LIMIT = 100;

  /** Consumer group prefix used for requeue/discard operations. */
  public static final String OPS_GROUP_PREFIX = "iped.dlq.ops.";

  private final String bootstrapServers;
  private final String dlqSuffix;
  private final AdminClient admin;

  public DlqManager(String bootstrapServers, String dlqSuffix) {
    this.bootstrapServers = bootstrapServers;
    this.dlqSuffix = dlqSuffix;
    Properties p = new Properties();
    p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    this.admin = AdminClient.create(p);
  }

  /** Test-only constructor — injects a (possibly mocked) {@link AdminClient} directly. */
  DlqManager(String dlqSuffix, AdminClient admin) {
    this.bootstrapServers = null;
    this.dlqSuffix = dlqSuffix;
    this.admin = admin;
  }

  // -----------------------------------------------------------------------
  // List (read-only peek)
  // -----------------------------------------------------------------------

  /**
   * Returns up to {@code maxPerTopic} DLQ entries from every DLQ topic belonging to {@code caseId}.
   * This is a non-destructive peek: no consumer offset is committed, so calling this method
   * multiple times always returns the same view.
   *
   * @param caseId the case identifier
   * @param maxPerTopic maximum entries to return per DLQ topic partition
   * @return list of DLQ entries, ordered by (topic, partition, offset)
   */
  public List<DlqEntry> list(String caseId, int maxPerTopic) throws Exception {
    Set<String> dlqTopics = dlqTopicsForCase(caseId);
    if (dlqTopics.isEmpty()) {
      log.debug("No DLQ topics found for case '{}'", caseId);
      return List.of();
    }

    List<DlqEntry> result = new ArrayList<>();
    // Use a random group so multiple simultaneous list calls don't interfere
    String peekGroup = "iped.dlq.peek." + UUID.randomUUID();
    try (KafkaConsumer<String, KafkaItemMessage> consumer = buildConsumer(peekGroup)) {
      List<TopicPartition> partitions = assignedPartitions(consumer, dlqTopics);
      if (partitions.isEmpty()) return List.of();

      consumer.seekToBeginning(partitions);
      Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
      Map<String, Integer> countPerTopic = new HashMap<>();

      long deadline = System.currentTimeMillis() + 10_000;
      while (System.currentTimeMillis() < deadline) {
        ConsumerRecords<String, KafkaItemMessage> records = consumer.poll(Duration.ofMillis(500));

        if (records.isEmpty()) {
          if (allAtEnd(consumer, partitions, endOffsets)) break;
          continue;
        }

        for (ConsumerRecord<String, KafkaItemMessage> rec : records) {
          if (rec.value() == null) continue;
          int seen = countPerTopic.getOrDefault(rec.topic(), 0);
          if (seen >= maxPerTopic) continue;
          result.add(DlqEntry.from(rec.topic(), rec.partition(), rec.offset(), rec.value()));
          countPerTopic.put(rec.topic(), seen + 1);
        }

        if (dlqTopics.stream().allMatch(t -> countPerTopic.getOrDefault(t, 0) >= maxPerTopic))
          break;
      }
    }

    result.sort(
        Comparator.comparing(DlqEntry::getDlqTopic)
            .thenComparingInt(DlqEntry::getPartition)
            .thenComparingLong(DlqEntry::getOffset));
    log.info("Listed {} DLQ entries for case '{}'", result.size(), caseId);
    return result;
  }

  // -----------------------------------------------------------------------
  // Requeue
  // -----------------------------------------------------------------------

  /**
   * Requeues the DLQ items at the given positions by republishing them to their original stage
   * topic with {@code attempt} reset to 0 (as if arriving fresh). Commits the DLQ offsets so these
   * items are not returned by future list/requeue calls.
   *
   * @param caseId the case identifier (used for the ops consumer group)
   * @param positions DLQ coordinates obtained from a prior {@link #list} call
   * @return number of items successfully requeued
   */
  public int requeue(String caseId, List<DlqPosition> positions) {
    return requeue(caseId, positions, 0L);
  }

  /**
   * Requeues the DLQ items at the given positions with an optional processing delay.
   *
   * @param caseId the case identifier
   * @param positions DLQ coordinates from a prior {@link #list} call
   * @param delayMs milliseconds to delay before the agent picks up the item; sets {@code
   *     notBeforeMs = now + delayMs}. {@code 0} = immediate.
   * @return number of items successfully requeued
   */
  public int requeue(String caseId, List<DlqPosition> positions, long delayMs) {
    if (positions.isEmpty()) return 0;
    int count = 0;
    try (KafkaConsumer<String, KafkaItemMessage> consumer =
            buildConsumer(OPS_GROUP_PREFIX + caseId);
        KafkaProducer<String, KafkaItemMessage> producer = buildProducer()) {
      count = processItems(consumer, positions, producer, delayMs);
    } catch (Exception ex) {
      log.error("Requeue operation failed for case '{}': {}", caseId, ex.getMessage(), ex);
    }
    log.info(
        "Requeued {}/{} DLQ items for case '{}' (delayMs={})",
        count,
        positions.size(),
        caseId,
        delayMs);
    return count;
  }

  // -----------------------------------------------------------------------
  // Discard
  // -----------------------------------------------------------------------

  /**
   * Discards the DLQ items at the given positions without republishing them. Commits the DLQ
   * offsets so these items are not returned by future list/discard calls.
   *
   * @param caseId the case identifier (used for the ops consumer group)
   * @param positions DLQ coordinates obtained from a prior {@link #list} call
   * @return number of items successfully discarded
   */
  public int discard(String caseId, List<DlqPosition> positions) {
    if (positions.isEmpty()) return 0;
    int count = 0;
    try (KafkaConsumer<String, KafkaItemMessage> consumer =
        buildConsumer(OPS_GROUP_PREFIX + caseId)) {
      count = processItems(consumer, positions, null, 0L);
    } catch (Exception ex) {
      log.error("Discard operation failed for case '{}': {}", caseId, ex.getMessage(), ex);
    }
    log.info("Discarded {}/{} DLQ items for case '{}'", count, positions.size(), caseId);
    return count;
  }

  /**
   * Returns per-topic statistics for all DLQ topics of the given case. Empty list when no DLQ
   * topics exist or the broker is unreachable.
   */
  public List<DlqTopicStats> topicStats(String caseId) {
    try {
      Set<String> dlqTopics = dlqTopicsForCase(caseId);
      if (dlqTopics.isEmpty()) return List.of();
      String peekGroup = "iped.dlq.stats." + java.util.UUID.randomUUID();
      List<DlqTopicStats> result = new ArrayList<>();
      try (KafkaConsumer<String, KafkaItemMessage> consumer = buildConsumer(peekGroup)) {
        for (String dlqTopic : dlqTopics) {
          List<PartitionInfo> infos = consumer.partitionsFor(dlqTopic);
          if (infos == null || infos.isEmpty()) continue;
          List<TopicPartition> tps =
              infos.stream()
                  .map(pi -> new TopicPartition(dlqTopic, pi.partition()))
                  .collect(Collectors.toList());
          consumer.assign(tps);
          Map<TopicPartition, Long> beginOffsets = consumer.beginningOffsets(tps);
          Map<TopicPartition, Long> endOffsets = consumer.endOffsets(tps);
          long count = 0, oldest = Long.MAX_VALUE, newest = Long.MIN_VALUE;
          for (TopicPartition tp : tps) {
            long b = beginOffsets.getOrDefault(tp, 0L);
            long e = endOffsets.getOrDefault(tp, 0L);
            count += Math.max(0L, e - b);
            if (e > b) {
              oldest = Math.min(oldest, b);
              newest = Math.max(newest, e - 1);
            }
          }
          if (oldest == Long.MAX_VALUE) oldest = -1;
          if (newest == Long.MIN_VALUE) newest = -1;
          result.add(
              new DlqTopicStats(
                  dlqTopic, DlqEntry.originalTopic(dlqTopic, dlqSuffix), count, oldest, newest));
        }
      }
      result.sort(java.util.Comparator.comparing(DlqTopicStats::dlqTopic));
      return result;
    } catch (Exception e) {
      log.debug("DLQ topicStats failed for case '{}': {}", caseId, e.getMessage());
      return List.of();
    }
  }

  /**
   * Returns the total number of unconsumed DLQ entries for the given case across all DLQ topics.
   * Lightweight alternative to {@link #list} for monitoring dashboards.
   *
   * <p>Implemented purely via the {@code AdminClient} already held by this instance (long-lived,
   * created once in the constructor) — it does <em>not</em> open a {@code KafkaConsumer} per call.
   *
   * <p><b>Prefer {@link #countForCases(Collection)}</b> when computing counts for multiple cases in
   * the same scrape (e.g. {@code CoordinatorServer.MetricsServlet}): this single-case method issues
   * its own {@code describeTopics}/{@code listOffsets} round-trips, so calling it once per case in
   * a loop is O(N) broker round-trips for N cases. {@link #countForCases(Collection)} batches all
   * cases into a small constant number of round-trips regardless of case count.
   *
   * @return entry count; {@code 0} when no DLQ topics exist or the broker is unreachable
   */
  public long count(String caseId) {
    Map<String, Long> result = countForCases(List.of(caseId));
    return result.getOrDefault(caseId, 0L);
  }

  /**
   * Explicit bound on the batched {@code describeTopics}/{@code listOffsets} round-trips issued by
   * {@link #countForCases(Collection)}, mirroring {@code BrokerHealthProbe}'s timeout discipline
   * (NFR-E2). No timeout previously existed on this path.
   */
  static final int COUNT_TIMEOUT_MS = 5000;

  /**
   * Batched, multi-case equivalent of {@link #count(String)}.
   *
   * <p>Computes unconsumed DLQ entry counts for every given case in a small constant number of
   * {@code AdminClient} round-trips — one {@code listTopics}, one {@code describeTopics} covering
   * every case's DLQ topics, and two {@code listOffsets} calls (earliest/latest) covering every
   * case's DLQ topic-partitions — instead of repeating that whole sequence once per case. This is
   * what {@code CoordinatorServer.MetricsServlet} should call on every Prometheus scrape, since a
   * per-case loop over {@link #count(String)} would issue O(active cases) blocking round-trips per
   * scrape.
   *
   * <p>Best-effort: on any broker error/timeout, returns {@code 0} for every requested case rather
   * than propagating an exception, matching {@link #count(String)}'s fail-open behavior.
   *
   * @param caseIds the case identifiers to compute DLQ counts for
   * @return map from caseId to entry count; every requested caseId is present (0 when no DLQ topics
   *     exist for it)
   */
  public Map<String, Long> countForCases(Collection<String> caseIds) {
    Map<String, Long> result = new LinkedHashMap<>();
    for (String caseId : caseIds) {
      result.put(caseId, 0L);
    }
    if (caseIds.isEmpty()) return result;

    try {
      // 1) Discover all DLQ topics across all requested cases in a single listTopics call,
      //    and record which case each topic belongs to (topic names encode the case id).
      Set<String> allTopicNames =
          admin
              .listTopics(
                  new org.apache.kafka.clients.admin.ListTopicsOptions()
                      .timeoutMs(COUNT_TIMEOUT_MS))
              .names()
              .get(COUNT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

      Map<String, String> topicToCaseId = new HashMap<>();
      for (String caseId : caseIds) {
        String prefix = TopicProvisioner.TOPIC_PREFIX + caseId + TopicProvisioner.STAGE_INFIX;
        for (String topic : allTopicNames) {
          if (topic.startsWith(prefix) && topic.endsWith(dlqSuffix)) {
            topicToCaseId.put(topic, caseId);
          }
        }
      }
      if (topicToCaseId.isEmpty()) return result;

      // 2) Describe every DLQ topic for every case in a single batched call.
      Map<String, org.apache.kafka.clients.admin.TopicDescription> descriptions =
          admin
              .describeTopics(
                  topicToCaseId.keySet(),
                  new org.apache.kafka.clients.admin.DescribeTopicsOptions()
                      .timeoutMs(COUNT_TIMEOUT_MS))
              .allTopicNames()
              .get(COUNT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

      Map<TopicPartition, String> partitionToCaseId = new LinkedHashMap<>();
      for (var descr : descriptions.values()) {
        String caseId = topicToCaseId.get(descr.name());
        if (caseId == null) continue;
        descr
            .partitions()
            .forEach(
                p ->
                    partitionToCaseId.put(new TopicPartition(descr.name(), p.partition()), caseId));
      }
      if (partitionToCaseId.isEmpty()) return result;

      // 3) Batch the earliest/latest offset lookups across every case's partitions
      //    into a single listOffsets call each, instead of one pair of calls per case.
      Map<TopicPartition, org.apache.kafka.clients.admin.OffsetSpec> earliestSpecs =
          new HashMap<>();
      Map<TopicPartition, org.apache.kafka.clients.admin.OffsetSpec> latestSpecs = new HashMap<>();
      for (TopicPartition tp : partitionToCaseId.keySet()) {
        earliestSpecs.put(tp, org.apache.kafka.clients.admin.OffsetSpec.earliest());
        latestSpecs.put(tp, org.apache.kafka.clients.admin.OffsetSpec.latest());
      }

      org.apache.kafka.clients.admin.ListOffsetsOptions offsetsOptions =
          new org.apache.kafka.clients.admin.ListOffsetsOptions().timeoutMs(COUNT_TIMEOUT_MS);

      Map<TopicPartition, org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo>
          beginOffsets =
              admin
                  .listOffsets(earliestSpecs, offsetsOptions)
                  .all()
                  .get(COUNT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
      Map<TopicPartition, org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo>
          endOffsets =
              admin
                  .listOffsets(latestSpecs, offsetsOptions)
                  .all()
                  .get(COUNT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

      for (var entry : partitionToCaseId.entrySet()) {
        TopicPartition tp = entry.getKey();
        String caseId = entry.getValue();
        var beginInfo = beginOffsets.get(tp);
        var endInfo = endOffsets.get(tp);
        if (beginInfo == null || endInfo == null) continue;
        long begin = beginInfo.offset();
        long end = endInfo.offset();
        if (end > begin) {
          result.merge(caseId, end - begin, Long::sum);
        }
      }
      return result;
    } catch (Exception e) {
      log.debug("Batched DLQ count failed for {} case(s): {}", caseIds.size(), e.getMessage());
      Map<String, Long> zeroed = new LinkedHashMap<>();
      for (String caseId : caseIds) zeroed.put(caseId, 0L);
      return zeroed;
    }
  }

  // -----------------------------------------------------------------------
  // Core item processing (shared by requeue and discard)
  // -----------------------------------------------------------------------

  /**
   * Seeks to each requested position, reads the record, optionally publishes it to the original
   * topic ({@code producer != null} → requeue, {@code null} → discard), and commits the DLQ offset.
   *
   * @param producer null means discard (no republishing)
   * @return count of successfully handled items
   */
  private int processItems(
      KafkaConsumer<String, KafkaItemMessage> consumer,
      List<DlqPosition> positions,
      KafkaProducer<String, KafkaItemMessage> producer,
      long delayMs) {
    // Group by partition for efficient seeking
    Map<TopicPartition, List<DlqPosition>> byPartition =
        positions.stream()
            .collect(
                Collectors.groupingBy(p -> new TopicPartition(p.getDlqTopic(), p.getPartition())));

    consumer.assign(new ArrayList<>(byPartition.keySet()));

    int handled = 0;
    for (Map.Entry<TopicPartition, List<DlqPosition>> e : byPartition.entrySet()) {
      TopicPartition tp = e.getKey();
      // Process positions in ascending offset order to commit correctly
      List<DlqPosition> sorted =
          e.getValue().stream()
              .sorted(Comparator.comparingLong(DlqPosition::getOffset))
              .collect(Collectors.toList());

      for (DlqPosition pos : sorted) {
        consumer.seek(tp, pos.getOffset());
        ConsumerRecords<String, KafkaItemMessage> records = consumer.poll(Duration.ofSeconds(5));

        boolean found = false;
        for (ConsumerRecord<String, KafkaItemMessage> rec : records) {
          if (rec.partition() != tp.partition() || rec.offset() != pos.getOffset()) continue;
          KafkaItemMessage msg = rec.value();
          if (msg == null) {
            log.warn("Null message at {}:{}", tp, pos.getOffset());
            break;
          }

          if (producer != null) {
            // Requeue: reset attempt and send to original stage topic
            msg.setAttempt(0);
            msg.setNotBeforeMs(delayMs > 0 ? System.currentTimeMillis() + delayMs : 0L);
            String origTopic = DlqEntry.originalTopic(pos.getDlqTopic(), dlqSuffix);
            try {
              producer.send(new ProducerRecord<>(origTopic, msg.getItemUuid(), msg)).get();
              log.info(
                  "Requeued item '{}' → '{}' (was stage={}, attempt={})",
                  msg.getItemUuid(),
                  origTopic,
                  rec.value().getPipelineStage(),
                  rec.value().getAttempt());
            } catch (Exception sendEx) {
              log.error("Failed to requeue item '{}': {}", msg.getItemUuid(), sendEx.getMessage());
              break; // don't commit — let operator retry
            }
          } else {
            log.info(
                "Discarded item '{}' from '{}' (stage={}, attempt={})",
                msg.getItemUuid(),
                pos.getDlqTopic(),
                msg.getPipelineStage(),
                msg.getAttempt());
          }

          // Commit past this offset in the ops consumer group
          consumer.commitSync(Map.of(tp, new OffsetAndMetadata(pos.getOffset() + 1)));
          handled++;
          found = true;
          break;
        }
        if (!found) {
          log.warn("Record not found at position {}; it may have already been processed", pos);
        }
      }
    }
    return handled;
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  /**
   * Returns the names of all DLQ topics that belong to the given case. A DLQ topic is any topic
   * whose name starts with {@code iped.{caseId}.stage.} and ends with the configured DLQ suffix.
   */
  Set<String> dlqTopicsForCase(String caseId) throws Exception {
    String prefix = TopicProvisioner.TOPIC_PREFIX + caseId + TopicProvisioner.STAGE_INFIX;
    return admin.listTopics().names().get().stream()
        .filter(n -> n.startsWith(prefix) && n.endsWith(dlqSuffix))
        .collect(Collectors.toSet());
  }

  private List<TopicPartition> assignedPartitions(
      KafkaConsumer<String, KafkaItemMessage> consumer, Set<String> topics) {
    List<TopicPartition> tps = new ArrayList<>();
    for (String topic : topics) {
      List<PartitionInfo> infos = consumer.partitionsFor(topic);
      if (infos != null) {
        for (PartitionInfo pi : infos) {
          tps.add(new TopicPartition(topic, pi.partition()));
        }
      }
    }
    consumer.assign(tps);
    return tps;
  }

  private boolean allAtEnd(
      KafkaConsumer<?, ?> consumer,
      List<TopicPartition> partitions,
      Map<TopicPartition, Long> endOffsets) {
    return partitions.stream()
        .allMatch(
            tp -> {
              long end = endOffsets.getOrDefault(tp, 0L);
              return consumer.position(tp) >= end;
            });
  }

  private KafkaConsumer<String, KafkaItemMessage> buildConsumer(String groupId) {
    Properties p = new Properties();
    p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaItemDeserializer.class.getName());
    p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
    return new KafkaConsumer<>(p);
  }

  private KafkaProducer<String, KafkaItemMessage> buildProducer() {
    Properties p = new Properties();
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaItemSerializer.class.getName());
    p.put(ProducerConfig.ACKS_CONFIG, "all");
    p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    return new KafkaProducer<>(p);
  }

  @Override
  public void close() {
    admin.close();
  }
}
