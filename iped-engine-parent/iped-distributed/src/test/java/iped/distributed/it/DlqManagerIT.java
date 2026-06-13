package iped.distributed.it;

import iped.distributed.kafka.DlqEntry;
import iped.distributed.kafka.DlqManager;
import iped.distributed.kafka.DlqPosition;
import iped.distributed.kafka.DlqTopicStats;
import iped.distributed.kafka.KafkaItemDeserializer;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.KafkaItemSerializer;
import iped.distributed.kafka.TopicProvisioner;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link DlqManager} — dead-letter queue list, count,
 * topicStats, requeue, and discard operations against a real Kafka broker.
 *
 * <p>Each test uses an isolated {@code caseId} prefix so topics never collide.
 * Topics are pre-created via {@link AdminClient} because the DLQ topic naming
 * pattern must match exactly what {@code DlqManager.dlqTopicsForCase} discovers.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class DlqManagerIT {

    private static final String DLQ_SUFFIX = ".dlq";
    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("apache/kafka:3.8.1");

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    // -----------------------------------------------------------------------
    // list
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void list_returnsEntriesFromDlqTopic() throws Exception {
        String caseId     = newCaseId("list");
        String dlqTopic   = dlqTopic(caseId, 0);
        String stageTopic = TopicProvisioner.stageTopic(caseId, 0);
        createTopics(stageTopic, dlqTopic);

        KafkaItemMessage msg1 = dlqMsg(caseId, 0, 3);
        KafkaItemMessage msg2 = dlqMsg(caseId, 0, 3);
        publishToDlq(dlqTopic, msg1, msg2);

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            List<DlqEntry> entries = mgr.list(caseId, 100);

            assertEquals(2, entries.size(), "list() must return 2 DLQ entries");
            for (DlqEntry e : entries) {
                assertEquals(dlqTopic, e.getDlqTopic());
                assertEquals(0, e.getPipelineStage());
                assertEquals(3, e.getAttempt());
                assertNotNull(e.getItemUuid());
            }
            Set<String> uuids = Set.of(entries.get(0).getItemUuid(), entries.get(1).getItemUuid());
            assertTrue(uuids.contains(msg1.getItemUuid()), "entry for msg1 must be present");
            assertTrue(uuids.contains(msg2.getItemUuid()), "entry for msg2 must be present");
        }
    }

    @Test
    @Timeout(120)
    void list_isIdempotent_neverCommitsOffset() throws Exception {
        String caseId   = newCaseId("list-idem");
        String dlqTopic = dlqTopic(caseId, 0);
        createTopics(TopicProvisioner.stageTopic(caseId, 0), dlqTopic);
        publishToDlq(dlqTopic, dlqMsg(caseId, 0, 2));

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            assertEquals(1, mgr.list(caseId, 100).size(), "first list() call");
            assertEquals(1, mgr.list(caseId, 100).size(),
                    "second list() call must see the same entry — list is non-destructive");
        }
    }

    // -----------------------------------------------------------------------
    // count
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void count_reflectsNumberOfDlqEntries() throws Exception {
        String caseId   = newCaseId("count");
        String dlqTopic = dlqTopic(caseId, 0);
        createTopics(TopicProvisioner.stageTopic(caseId, 0), dlqTopic);
        publishToDlq(dlqTopic, dlqMsg(caseId, 0, 2), dlqMsg(caseId, 0, 2), dlqMsg(caseId, 0, 2));

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            assertEquals(3L, mgr.count(caseId),
                    "count() must equal the number of messages published to the DLQ");
        }
    }

    @Test
    @Timeout(120)
    void count_returnsZero_whenNoDlqTopicsExist() throws Exception {
        String caseId = newCaseId("count-empty");
        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            assertEquals(0L, mgr.count(caseId),
                    "count() must return 0 when no DLQ topic exists for the case");
        }
    }

    // -----------------------------------------------------------------------
    // topicStats
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void topicStats_returnsCorrectOffsetRange() throws Exception {
        String caseId     = newCaseId("stats");
        String dlqTopic   = dlqTopic(caseId, 0);
        String stageTopic = TopicProvisioner.stageTopic(caseId, 0);
        createTopics(stageTopic, dlqTopic);
        publishToDlq(dlqTopic, dlqMsg(caseId, 0, 2), dlqMsg(caseId, 0, 2));

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            List<DlqTopicStats> stats = mgr.topicStats(caseId);

            assertEquals(1, stats.size(), "one DLQ topic for this case → one stats entry");
            DlqTopicStats s = stats.get(0);
            assertEquals(dlqTopic,   s.dlqTopic(),      "dlqTopic field");
            assertEquals(stageTopic, s.originalTopic(),  "originalTopic by stripping .dlq suffix");
            assertEquals(2L,         s.count(),          "count must equal messages published");
            assertEquals(0L,         s.oldestOffset(),   "oldest offset = 0 (first message)");
            assertEquals(1L,         s.newestOffset(),   "newest offset = 1 (second message)");
        }
    }

    // -----------------------------------------------------------------------
    // requeue
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void requeue_sendsItemToOriginalStageTopicWithAttemptReset() throws Exception {
        String caseId     = newCaseId("requeue");
        String dlqTopic   = dlqTopic(caseId, 0);
        String stageTopic = TopicProvisioner.stageTopic(caseId, 0);
        createTopics(stageTopic, dlqTopic);

        KafkaItemMessage original = dlqMsg(caseId, 0, 3);
        publishToDlq(dlqTopic, original);

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            List<DlqEntry> entries = mgr.list(caseId, 100);
            assertEquals(1, entries.size());
            DlqEntry entry = entries.get(0);

            int requeued = mgr.requeue(caseId,
                    List.of(new DlqPosition(entry.getDlqTopic(), entry.getPartition(), entry.getOffset())));
            assertEquals(1, requeued, "requeue() must report 1 item requeued");
        }

        KafkaItemMessage received = consumeOne(stageTopic, "requeue-verify-" + UUID.randomUUID());
        assertNotNull(received, "requeued item must appear on the original stage topic");
        assertEquals(original.getItemUuid(), received.getItemUuid(),
                "UUID must survive the requeue round-trip");
        assertEquals(0, received.getAttempt(),
                "attempt counter must be reset to 0 on requeue");
    }

    @Test
    @Timeout(120)
    void requeue_withDelay_setsNotBeforeMs() throws Exception {
        String caseId     = newCaseId("requeue-delay");
        String dlqTopic   = dlqTopic(caseId, 0);
        String stageTopic = TopicProvisioner.stageTopic(caseId, 0);
        createTopics(stageTopic, dlqTopic);

        publishToDlq(dlqTopic, dlqMsg(caseId, 0, 1));

        long delayMs      = 10_000L;
        long beforeRequeue = System.currentTimeMillis();

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            DlqEntry entry = mgr.list(caseId, 100).get(0);
            mgr.requeue(caseId,
                    List.of(new DlqPosition(entry.getDlqTopic(), entry.getPartition(), entry.getOffset())),
                    delayMs);
        }

        KafkaItemMessage received = consumeOne(stageTopic, "delay-verify-" + UUID.randomUUID());
        assertNotNull(received);
        assertTrue(received.getNotBeforeMs() >= beforeRequeue + delayMs,
                "notBeforeMs must be at least now+delayMs; got notBeforeMs="
                        + received.getNotBeforeMs() + " beforeRequeue=" + beforeRequeue
                        + " delayMs=" + delayMs);
    }

    @Test
    @Timeout(120)
    void requeue_multipleItems_allArriveOnStageTopic() throws Exception {
        String caseId     = newCaseId("requeue-multi");
        String dlqTopic   = dlqTopic(caseId, 0);
        String stageTopic = TopicProvisioner.stageTopic(caseId, 0);
        createTopics(stageTopic, dlqTopic);

        KafkaItemMessage m1 = dlqMsg(caseId, 0, 2);
        KafkaItemMessage m2 = dlqMsg(caseId, 0, 2);
        KafkaItemMessage m3 = dlqMsg(caseId, 0, 2);
        publishToDlq(dlqTopic, m1, m2, m3);

        List<DlqPosition> positions = new ArrayList<>();
        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            for (DlqEntry e : mgr.list(caseId, 100)) {
                positions.add(new DlqPosition(e.getDlqTopic(), e.getPartition(), e.getOffset()));
            }
            int count = mgr.requeue(caseId, positions);
            assertEquals(3, count, "all 3 items must be reported as requeued");
        }

        List<KafkaItemMessage> received = consumeN(stageTopic, "multi-verify-" + UUID.randomUUID(), 3);
        assertEquals(3, received.size(), "all 3 items must arrive on the original stage topic");
        Set<String> receivedUuids = Set.of(
                received.get(0).getItemUuid(),
                received.get(1).getItemUuid(),
                received.get(2).getItemUuid());
        assertTrue(receivedUuids.contains(m1.getItemUuid()), "m1 must be requeued");
        assertTrue(receivedUuids.contains(m2.getItemUuid()), "m2 must be requeued");
        assertTrue(receivedUuids.contains(m3.getItemUuid()), "m3 must be requeued");
    }

    // -----------------------------------------------------------------------
    // discard
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void discard_itemIsNotForwardedToStageTopic() throws Exception {
        String caseId     = newCaseId("discard");
        String dlqTopic   = dlqTopic(caseId, 0);
        String stageTopic = TopicProvisioner.stageTopic(caseId, 0);
        createTopics(stageTopic, dlqTopic);

        KafkaItemMessage toDiscard = dlqMsg(caseId, 0, 3);
        KafkaItemMessage toRequeue = dlqMsg(caseId, 0, 3);
        publishToDlq(dlqTopic, toDiscard, toRequeue);

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            List<DlqEntry> entries = mgr.list(caseId, 100);
            assertEquals(2, entries.size());
            // Entries are sorted by (topic, partition, offset), so offset 0 = toDiscard, 1 = toRequeue
            DlqEntry entryDiscard = entries.get(0);
            DlqEntry entryRequeue = entries.get(1);

            int discarded = mgr.discard(caseId,
                    List.of(new DlqPosition(entryDiscard.getDlqTopic(),
                            entryDiscard.getPartition(), entryDiscard.getOffset())));
            assertEquals(1, discarded, "discard() must report 1 item discarded");

            int requeued = mgr.requeue(caseId,
                    List.of(new DlqPosition(entryRequeue.getDlqTopic(),
                            entryRequeue.getPartition(), entryRequeue.getOffset())));
            assertEquals(1, requeued, "requeue() must report 1 item requeued");
        }

        // Only the requeued item must appear on the stage topic
        List<KafkaItemMessage> received = consumeN(stageTopic, "discard-verify-" + UUID.randomUUID(), 1);
        assertEquals(1, received.size(),
                "exactly one item must arrive on stage topic — the discarded one must not be forwarded");
        assertEquals(toRequeue.getItemUuid(), received.get(0).getItemUuid(),
                "the requeued item's UUID must match");
        assertNotEquals(toDiscard.getItemUuid(), received.get(0).getItemUuid(),
                "the discarded item must not appear on the stage topic");
    }

    // -----------------------------------------------------------------------
    // Multi-stage DLQ discovery
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void multiStage_listAndCount_aggregateAcrossBothDlqTopics() throws Exception {
        String caseId      = newCaseId("multi-stage");
        String stage0Topic = TopicProvisioner.stageTopic(caseId, 0);
        String stage1Topic = TopicProvisioner.stageTopic(caseId, 1);
        String dlq0        = dlqTopic(caseId, 0);
        String dlq1        = dlqTopic(caseId, 1);
        createTopics(stage0Topic, stage1Topic, dlq0, dlq1);

        KafkaItemMessage failedAtStage0 = dlqMsg(caseId, 0, 3);
        KafkaItemMessage failedAtStage1 = dlqMsg(caseId, 1, 2);
        publishToDlq(dlq0, failedAtStage0);
        publishToDlq(dlq1, failedAtStage1);

        try (DlqManager mgr = new DlqManager(bootstrapServers(), DLQ_SUFFIX)) {
            List<DlqEntry> entries = mgr.list(caseId, 100);
            assertEquals(2, entries.size(),
                    "list() must aggregate entries from both DLQ topics");
            Set<String> topics = Set.of(entries.get(0).getDlqTopic(), entries.get(1).getDlqTopic());
            assertTrue(topics.contains(dlq0), "entry from stage-0 DLQ must be present");
            assertTrue(topics.contains(dlq1), "entry from stage-1 DLQ must be present");

            // Each entry's stage field must reflect where it failed
            DlqEntry e0 = entries.stream()
                    .filter(e -> e.getDlqTopic().equals(dlq0)).findFirst().orElseThrow();
            DlqEntry e1 = entries.stream()
                    .filter(e -> e.getDlqTopic().equals(dlq1)).findFirst().orElseThrow();
            assertEquals(0, e0.getPipelineStage(), "stage-0 DLQ entry must carry pipelineStage=0");
            assertEquals(1, e1.getPipelineStage(), "stage-1 DLQ entry must carry pipelineStage=1");

            assertEquals(2L, mgr.count(caseId),
                    "count() must sum entries across both DLQ topics");

            List<DlqTopicStats> stats = mgr.topicStats(caseId);
            assertEquals(2, stats.size(), "topicStats() must return one entry per DLQ topic");
            Set<String> statTopics = Set.of(stats.get(0).dlqTopic(), stats.get(1).dlqTopic());
            assertTrue(statTopics.contains(dlq0), "stats for stage-0 DLQ must be present");
            assertTrue(statTopics.contains(dlq1), "stats for stage-1 DLQ must be present");
            stats.forEach(s -> assertEquals(1L, s.count(),
                    "each DLQ topic must report exactly 1 entry"));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String bootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    private static String newCaseId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String dlqTopic(String caseId, int stage) {
        return TopicProvisioner.stageTopic(caseId, stage) + DLQ_SUFFIX;
    }

    /** Creates all named topics (1 partition, replication factor 1) on the broker. */
    private static void createTopics(String... topics) throws Exception {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        try (AdminClient admin = AdminClient.create(p)) {
            List<NewTopic> newTopics = new ArrayList<>();
            for (String topic : topics) newTopics.add(new NewTopic(topic, 1, (short) 1));
            admin.createTopics(newTopics).all().get(30, TimeUnit.SECONDS);
        }
    }

    /** Builds a DLQ-style KafkaItemMessage with the given stage and attempt. */
    private static KafkaItemMessage dlqMsg(String caseId, int stage, int attempt) {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setCaseId(caseId);
        msg.setItemUuid(UUID.randomUUID().toString());
        msg.setPath("/evidence/" + caseId + "/item.bin");
        msg.setPipelineStage(stage);
        msg.setAttempt(attempt);
        msg.setLength(512L);
        return msg;
    }

    /** Publishes the given messages to {@code topic} synchronously. */
    @SafeVarargs
    private static void publishToDlq(String topic, KafkaItemMessage... messages) throws Exception {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaItemSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        try (KafkaProducer<String, KafkaItemMessage> producer = new KafkaProducer<>(p)) {
            for (KafkaItemMessage msg : messages) {
                producer.send(new ProducerRecord<>(topic, msg.getItemUuid(), msg))
                        .get(30, TimeUnit.SECONDS);
            }
        }
    }

    /** Polls {@code topic} until one full message is available. */
    private static KafkaItemMessage consumeOne(String topic, String groupId) {
        List<KafkaItemMessage> msgs = consumeN(topic, groupId, 1);
        return msgs.isEmpty() ? null : msgs.get(0);
    }

    /** Polls {@code topic} until at least {@code n} full messages are available. */
    private static List<KafkaItemMessage> consumeN(String topic, String groupId, int n) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaItemDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        List<KafkaItemMessage> result = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        try (KafkaConsumer<String, KafkaItemMessage> consumer = new KafkaConsumer<>(p)) {
            consumer.subscribe(List.of(topic));
            while (result.size() < n && System.nanoTime() < deadline) {
                for (ConsumerRecord<String, KafkaItemMessage> rec :
                        consumer.poll(Duration.ofMillis(500))) {
                    if (rec.value() != null) result.add(rec.value());
                }
            }
        }
        return result;
    }
}
