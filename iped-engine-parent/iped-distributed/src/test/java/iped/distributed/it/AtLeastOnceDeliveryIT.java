package iped.distributed.it;

import iped.distributed.kafka.KafkaItemDeserializer;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.KafkaItemSerializer;
import iped.distributed.kafka.PartitionOffsetTracker;
import iped.distributed.kafka.TopicProvisioner;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link PartitionOffsetTracker} against a real Kafka broker.
 *
 * <p>The broker-free chaos tests ({@code MultiAgentChaosTest}) verify the watermark
 * arithmetic in isolation.  These tests verify the full at-least-once guarantee
 * end-to-end: a consumer that commits only the watermark returned by
 * {@link PartitionOffsetTracker#drainCommittable} causes exactly the uncommitted
 * records to be redelivered to a new consumer in the same group.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class AtLeastOnceDeliveryIT {

    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("apache/kafka:3.8.1");

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    // -----------------------------------------------------------------------
    // Core at-least-once guarantee
    // -----------------------------------------------------------------------

    /**
     * Publishes 5 items, processes only the first 3 (commits watermark offset 3),
     * then verifies that a new consumer in the same group is delivered exactly items
     * 3 and 4 — neither more (items 0-2 are not redelivered) nor fewer.
     */
    @Test
    @Timeout(120)
    void uncommittedRecords_areRedeliveredToNextConsumerInSameGroup() throws Exception {
        String topic   = TopicProvisioner.stageTopic(newCaseId("aalo"), 0);
        String groupId = "aalo-group-" + UUID.randomUUID();
        createTopic(topic);

        // Publish 5 items with stable UUIDs so we can assert which ones come back
        String[] uuids = new String[5];
        try (KafkaProducer<String, KafkaItemMessage> producer = newProducer()) {
            for (int i = 0; i < 5; i++) {
                uuids[i] = UUID.randomUUID().toString();
                KafkaItemMessage msg = item(uuids[i]);
                producer.send(new ProducerRecord<>(topic, uuids[i], msg)).get(30, TimeUnit.SECONDS);
            }
        }

        // --- Consumer 1: poll all 5, mark only 0-2 as done, commit watermark ---
        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        TopicPartition tp = new TopicPartition(topic, 0);

        try (KafkaConsumer<String, KafkaItemMessage> consumer1 = newConsumer(groupId)) {
            consumer1.subscribe(List.of(topic));
            awaitAssignment(consumer1);

            List<ConsumerRecord<String, KafkaItemMessage>> all = drainN(consumer1, 5);
            assertEquals(5, all.size(), "consumer 1 must receive all 5 published records");

            // Register all 5 as polled so the tracker knows about the full window
            for (ConsumerRecord<String, KafkaItemMessage> rec : all) {
                tracker.recordPolled(tp, rec.offset());
            }
            // Mark only the first 3 as done — records 3 and 4 remain in-flight
            tracker.markDone(tp, all.get(0).offset());
            tracker.markDone(tp, all.get(1).offset());
            tracker.markDone(tp, all.get(2).offset());

            Map<TopicPartition, OffsetAndMetadata> toCommit = tracker.drainCommittable();
            assertEquals(1, toCommit.size(), "one partition must be committable");
            // Watermark advances through the consecutive run 0→1→2; commit value = offset(2)+1
            long expectedCommitOffset = all.get(2).offset() + 1;
            assertEquals(expectedCommitOffset, toCommit.get(tp).offset(),
                    "committed offset must be the offset after the last consecutively-completed record");

            consumer1.commitSync(toCommit);
        } // consumer1.close() — releases partition, group offset stays committed

        // --- Consumer 2: same group, must receive only items 3 and 4 ---
        List<KafkaItemMessage> redelivered = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        try (KafkaConsumer<String, KafkaItemMessage> consumer2 = newConsumer(groupId)) {
            consumer2.subscribe(List.of(topic));
            awaitAssignment(consumer2);
            // Collect everything that arrives within the deadline window
            while (System.nanoTime() < deadline) {
                ConsumerRecords<String, KafkaItemMessage> records =
                        consumer2.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, KafkaItemMessage> rec : records) {
                    if (rec.value() != null) redelivered.add(rec.value());
                }
                // Stop as soon as we've received 2 records (the two uncommitted ones)
                // or the poll window comes back empty twice in a row, meaning there is
                // nothing left to deliver — this guards against waiting forever.
                if (redelivered.size() >= 2 && records.isEmpty()) break;
                if (redelivered.size() >= 2) break;
            }
        }

        assertEquals(2, redelivered.size(),
                "consumer 2 must receive exactly 2 records — the uncommitted ones (offsets 3 & 4)");
        Set<String> redeliveredUuids = new HashSet<>();
        redelivered.forEach(m -> redeliveredUuids.add(m.getItemUuid()));

        // Items 3 and 4 must be redelivered
        assertTrue(redeliveredUuids.contains(uuids[3]), "item at offset 3 must be redelivered");
        assertTrue(redeliveredUuids.contains(uuids[4]), "item at offset 4 must be redelivered");

        // Items 0-2 must NOT be redelivered (their offsets were committed)
        assertFalse(redeliveredUuids.contains(uuids[0]), "committed item 0 must not be redelivered");
        assertFalse(redeliveredUuids.contains(uuids[1]), "committed item 1 must not be redelivered");
        assertFalse(redeliveredUuids.contains(uuids[2]), "committed item 2 must not be redelivered");
    }

    /**
     * Verifies the gap-stop invariant: when an out-of-order completion leaves a gap,
     * the watermark stops at the gap and the records after the gap are re-delivered on
     * restart — even though they were individually completed before the gap was filled.
     *
     * <p>Setup: 4 records at offsets 0-3.  Offsets 0, 2, 3 are marked done; offset 1
     * is NOT.  The watermark can only advance to 0 (the consecutive run stops at 1).
     * Committed offset = 1 (next-to-read).  On restart, records 1, 2, 3 are redelivered.
     */
    @Test
    @Timeout(120)
    void gap_stopsWatermark_andGapRecordIsRedeliveredWithSuccessors() throws Exception {
        String topic   = TopicProvisioner.stageTopic(newCaseId("gap"), 0);
        String groupId = "gap-group-" + UUID.randomUUID();
        createTopic(topic);

        String[] uuids = new String[4];
        try (KafkaProducer<String, KafkaItemMessage> producer = newProducer()) {
            for (int i = 0; i < 4; i++) {
                uuids[i] = UUID.randomUUID().toString();
                producer.send(new ProducerRecord<>(topic, uuids[i], item(uuids[i])))
                        .get(30, TimeUnit.SECONDS);
            }
        }

        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        TopicPartition tp = new TopicPartition(topic, 0);

        try (KafkaConsumer<String, KafkaItemMessage> consumer1 = newConsumer(groupId)) {
            consumer1.subscribe(List.of(topic));
            awaitAssignment(consumer1);

            List<ConsumerRecord<String, KafkaItemMessage>> all = drainN(consumer1, 4);
            assertEquals(4, all.size());
            for (ConsumerRecord<String, KafkaItemMessage> rec : all) tracker.recordPolled(tp, rec.offset());

            // Mark 0, 2, 3 done — leave offset 1 as a gap
            tracker.markDone(tp, all.get(0).offset()); // 0 ✓
            // offset 1 intentionally not marked
            tracker.markDone(tp, all.get(2).offset()); // 2 ✓
            tracker.markDone(tp, all.get(3).offset()); // 3 ✓

            Map<TopicPartition, OffsetAndMetadata> toCommit = tracker.drainCommittable();
            assertEquals(1, toCommit.size(), "watermark must advance only through offset 0");
            // Watermark = offset(0), commit value = offset(0)+1
            long expectedCommit = all.get(0).offset() + 1;
            assertEquals(expectedCommit, toCommit.get(tp).offset(),
                    "watermark must stop at the gap (offset 1 not done)");
            consumer1.commitSync(toCommit);
        }

        // Consumer 2 must receive records 1, 2, 3 (committed only through offset 0)
        List<KafkaItemMessage> redelivered = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        try (KafkaConsumer<String, KafkaItemMessage> consumer2 = newConsumer(groupId)) {
            consumer2.subscribe(List.of(topic));
            awaitAssignment(consumer2);
            while (redelivered.size() < 3 && System.nanoTime() < deadline) {
                for (ConsumerRecord<String, KafkaItemMessage> rec :
                        consumer2.poll(Duration.ofMillis(500))) {
                    if (rec.value() != null) redelivered.add(rec.value());
                }
            }
        }

        assertEquals(3, redelivered.size(),
                "records 1, 2, 3 must be redelivered — watermark stopped before the gap");
        Set<String> redeliveredUuids = new HashSet<>();
        redelivered.forEach(m -> redeliveredUuids.add(m.getItemUuid()));

        assertFalse(redeliveredUuids.contains(uuids[0]),
                "offset 0 was committed and must not be redelivered");
        assertTrue(redeliveredUuids.contains(uuids[1]),
                "gap record (offset 1) must be redelivered");
        assertTrue(redeliveredUuids.contains(uuids[2]),
                "record after gap (offset 2) must be redelivered — watermark stopped before it");
        assertTrue(redeliveredUuids.contains(uuids[3]),
                "record after gap (offset 3) must be redelivered");
    }

    /**
     * Verifies that after all records are marked done, {@link PartitionOffsetTracker#drainCommittable}
     * returns the full-commit offset and a restarted consumer in the same group receives nothing.
     */
    @Test
    @Timeout(120)
    void allRecordsCommitted_noRedeliveryOnRestart() throws Exception {
        String topic   = TopicProvisioner.stageTopic(newCaseId("nodupe"), 0);
        String groupId = "nodupe-group-" + UUID.randomUUID();
        createTopic(topic);

        String[] uuids = new String[3];
        try (KafkaProducer<String, KafkaItemMessage> producer = newProducer()) {
            for (int i = 0; i < 3; i++) {
                uuids[i] = UUID.randomUUID().toString();
                producer.send(new ProducerRecord<>(topic, uuids[i], item(uuids[i])))
                        .get(30, TimeUnit.SECONDS);
            }
        }

        PartitionOffsetTracker tracker = new PartitionOffsetTracker();
        TopicPartition tp = new TopicPartition(topic, 0);

        try (KafkaConsumer<String, KafkaItemMessage> consumer1 = newConsumer(groupId)) {
            consumer1.subscribe(List.of(topic));
            awaitAssignment(consumer1);

            List<ConsumerRecord<String, KafkaItemMessage>> all = drainN(consumer1, 3);
            assertEquals(3, all.size());
            for (ConsumerRecord<String, KafkaItemMessage> rec : all) {
                tracker.recordPolled(tp, rec.offset());
                tracker.markDone(tp, rec.offset());
            }
            Map<TopicPartition, OffsetAndMetadata> toCommit = tracker.drainCommittable();
            assertEquals(1, toCommit.size());
            consumer1.commitSync(toCommit);
        }

        // Consumer 2 should see nothing — all offsets committed
        List<KafkaItemMessage> redelivered = new ArrayList<>();
        try (KafkaConsumer<String, KafkaItemMessage> consumer2 = newConsumer(groupId)) {
            consumer2.subscribe(List.of(topic));
            awaitAssignment(consumer2);
            // Poll for 3 seconds; expect nothing
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, KafkaItemMessage> rec :
                        consumer2.poll(Duration.ofMillis(500))) {
                    if (rec.value() != null) redelivered.add(rec.value());
                }
            }
        }

        assertTrue(redelivered.isEmpty(),
                "no records must be redelivered when all offsets were committed");
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

    private static void createTopic(String topic) throws Exception {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        try (AdminClient admin = AdminClient.create(p)) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1)))
                 .all().get(30, TimeUnit.SECONDS);
        }
    }

    private static KafkaItemMessage item(String uuid) {
        KafkaItemMessage m = new KafkaItemMessage();
        m.setItemUuid(uuid);
        m.setCaseId("delivery-test");
        m.setPipelineStage(0);
        m.setLength(128L);
        return m;
    }

    private static KafkaProducer<String, KafkaItemMessage> newProducer() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaItemSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new KafkaProducer<>(p);
    }

    private static KafkaConsumer<String, KafkaItemMessage> newConsumer(String groupId) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaItemDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(p);
    }

    private static void awaitAssignment(KafkaConsumer<?, ?> consumer) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            consumer.poll(Duration.ofMillis(100));
            if (!consumer.assignment().isEmpty()) return;
        }
        fail("consumer did not receive a partition assignment in time");
    }

    /** Polls until exactly {@code n} records have been collected, then returns them. */
    private static List<ConsumerRecord<String, KafkaItemMessage>> drainN(
            KafkaConsumer<String, KafkaItemMessage> consumer, int n) {
        List<ConsumerRecord<String, KafkaItemMessage>> result = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (result.size() < n && System.nanoTime() < deadline) {
            for (ConsumerRecord<String, KafkaItemMessage> rec :
                    consumer.poll(Duration.ofMillis(500))) {
                if (rec.value() != null) result.add(rec);
            }
        }
        return result;
    }
}
