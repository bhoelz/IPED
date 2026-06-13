package iped.distributed.it;

import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.status.ItemStatusConsumer;
import iped.distributed.status.ItemStatusEvent;
import iped.distributed.status.ItemStatusProducer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the status topic ({@code iped.status}) — producer/consumer
 * round-trip, consumer-group fan-out, agentId stamping, event-type coverage, and
 * monitor recovery from a replayed topic.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class StatusTopicIT {

    private static final String HASH_TASK = "HashTask";
    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("apache/kafka:3.8.1");

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    // -----------------------------------------------------------------------
    // Fan-out: two independent consumer groups both receive all events
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void twoConsumerGroups_bothReceiveAllEvents() throws Exception {
        String caseId = newCaseId("fanout");
        KafkaItemMessage msg = item(caseId, 0);

        // Two latches — each group must receive DISCOVERED + STARTED + COMPLETED
        CountDownLatch latch1 = new CountDownLatch(3);
        CountDownLatch latch2 = new CountDownLatch(3);

        ItemStatusConsumer consumer1 = new ItemStatusConsumer(
                bootstrapServers(), "fanout-A-" + UUID.randomUUID(), true,
                e -> { if (caseId.equals(e.getCaseId())) latch1.countDown(); });
        ItemStatusConsumer consumer2 = new ItemStatusConsumer(
                bootstrapServers(), "fanout-B-" + UUID.randomUUID(), true,
                e -> { if (caseId.equals(e.getCaseId())) latch2.countDown(); });

        try {
            consumer1.start();
            consumer2.start();
            Thread.sleep(1000); // let consumers join the topic before events are published

            ItemStatusProducer producer = new ItemStatusProducer(bootstrapServers());
            try {
                producer.publishDiscovered(msg);
                producer.publishStarted(msg, HASH_TASK);
                producer.publishCompleted(msg, HASH_TASK, 10L);
            } finally {
                producer.close();
            }

            assertTrue(latch1.await(30, TimeUnit.SECONDS),
                    "consumer group A must receive all 3 events");
            assertTrue(latch2.await(30, TimeUnit.SECONDS),
                    "consumer group B must receive all 3 events — Kafka fan-out");
        } finally {
            consumer1.close();
            consumer2.close();
        }
    }

    // -----------------------------------------------------------------------
    // agentId stamping: producer with agentId vs. coordinator-mode producer
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void agentId_isStampedOnEventsWhenProducerHasAgentId() throws Exception {
        String caseId  = newCaseId("agentid");
        String agentId = "forensic-agent-007";

        List<ItemStatusEvent> received = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        ItemStatusConsumer consumer = new ItemStatusConsumer(
                bootstrapServers(), "agentid-" + UUID.randomUUID(), true,
                e -> { if (caseId.equals(e.getCaseId())) { received.add(e); latch.countDown(); } });

        try {
            consumer.start();
            Thread.sleep(1000);

            KafkaItemMessage msg = item(caseId, 0);
            // 2-arg constructor: every event gets agentId stamped
            ItemStatusProducer agentProducer = new ItemStatusProducer(bootstrapServers(), agentId);
            // 1-arg constructor: coordinator usage — agentId must be null
            ItemStatusProducer coordProducer = new ItemStatusProducer(bootstrapServers());
            try {
                agentProducer.publishDiscovered(msg);
                coordProducer.publishStarted(msg, HASH_TASK);
            } finally {
                agentProducer.close();
                coordProducer.close();
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "both events must arrive");

            ItemStatusEvent discovered = received.stream()
                    .filter(e -> e.getType() == ItemStatusEvent.Type.DISCOVERED)
                    .findFirst().orElseThrow();
            ItemStatusEvent started = received.stream()
                    .filter(e -> e.getType() == ItemStatusEvent.Type.STARTED)
                    .findFirst().orElseThrow();

            assertEquals(agentId, discovered.getAgentId(),
                    "DISCOVERED published by agent producer must carry agentId");
            assertNull(started.getAgentId(),
                    "STARTED published by coordinator producer must have null agentId");
        } finally {
            consumer.close();
        }
    }

    // -----------------------------------------------------------------------
    // All major event types survive serialization through Kafka
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void allMajorEventTypes_roundTripThroughStatusTopic() throws Exception {
        String caseId = newCaseId("evttypes");
        KafkaItemMessage msg = item(caseId, 0);

        // 7 distinct event types we want to observe
        Set<ItemStatusEvent.Type> expected = EnumSet.of(
                ItemStatusEvent.Type.DISCOVERED,
                ItemStatusEvent.Type.SUBITEM_DISCOVERED,
                ItemStatusEvent.Type.STARTED,
                ItemStatusEvent.Type.COMPLETED,
                ItemStatusEvent.Type.ERROR,
                ItemStatusEvent.Type.SKIPPED,
                ItemStatusEvent.Type.CASE_COMPLETED);

        Set<ItemStatusEvent.Type> observed = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(expected.size());

        ItemStatusConsumer consumer = new ItemStatusConsumer(
                bootstrapServers(), "evttypes-" + UUID.randomUUID(), true,
                e -> {
                    if (!caseId.equals(e.getCaseId())) return;
                    synchronized (observed) {
                        if (observed.add(e.getType())) latch.countDown();
                    }
                });

        try {
            consumer.start();
            Thread.sleep(1000);

            ItemStatusProducer producer = new ItemStatusProducer(bootstrapServers());
            try {
                producer.publishDiscovered(msg);
                producer.publishSubitemDiscovered(item(caseId, 0));
                producer.publishStarted(msg, HASH_TASK);
                producer.publishCompleted(msg, HASH_TASK, 5L);
                producer.publishError(item(caseId, 0), HASH_TASK, 3L, new RuntimeException("test-err"));
                producer.publishSkipped(item(caseId, 0), HASH_TASK);
                producer.publishCaseCompleted(caseId);
            } finally {
                producer.close();
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS),
                    "all 7 event types must be received; missing: "
                            + expected.stream().filter(t -> !observed.contains(t))
                                      .collect(Collectors.toSet()));
        } finally {
            consumer.close();
        }
    }

    // -----------------------------------------------------------------------
    // Monitor recovery: fromBeginning=true rebuilds state from full history
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void monitorRecovery_fromBeginning_rebuildsCorrectState() throws Exception {
        String caseId = newCaseId("recovery");
        KafkaItemMessage msg = item(caseId, 0);

        // Phase 1: publish a complete case lifecycle to iped.status via a short-lived producer
        ItemStatusProducer producer = new ItemStatusProducer(bootstrapServers());
        try {
            producer.publishDiscovered(msg);
            producer.publishStarted(msg, HASH_TASK);
            producer.publishCompleted(msg, HASH_TASK, 10L);
        } finally {
            producer.close();
        }
        // Brief pause so Kafka offsets are committed on the broker before we start recovering
        Thread.sleep(500);

        // Phase 2: build a fresh lifecycle + monitor and replay from offset 0
        TopicProvisioner noopProvisioner = new TopicProvisioner(bootstrapServers()) {
            @Override public void provisionCase(String id, int n, int p, short r) {}
        };
        CaseLifecycleManager lifecycle = new CaseLifecycleManager(noopProvisioner);
        lifecycle.startCase(caseId, List.of(HASH_TASK), 1, (short) 1);

        List<ItemStatusEvent> replayed = new CopyOnWriteArrayList<>();
        CountDownLatch completedLatch = new CountDownLatch(1);
        CaseCompletionMonitor monitor = new CaseCompletionMonitor(lifecycle,
                e -> {
                    replayed.add(e);
                    if (e.getType() == ItemStatusEvent.Type.CASE_COMPLETED) {
                        lifecycle.completeCase(e.getCaseId());
                        completedLatch.countDown();
                    }
                },
                3600);

        // fromBeginning=true so the consumer re-reads the topic from offset 0
        ItemStatusConsumer recovery = new ItemStatusConsumer(
                bootstrapServers(), "recovery-" + UUID.randomUUID(), true, monitor::onEvent);
        try {
            recovery.start();
            assertTrue(completedLatch.await(30, TimeUnit.SECONDS),
                    "monitor must detect case completion while replaying the status topic from the beginning");
            assertTrue(monitor.isCompleted(caseId),
                    "monitor.isCompleted must be true after replay");
            assertTrue(replayed.stream().anyMatch(e -> e.getType() == ItemStatusEvent.Type.DISCOVERED));
            assertTrue(replayed.stream().anyMatch(e -> e.getType() == ItemStatusEvent.Type.STARTED));
            assertTrue(replayed.stream().anyMatch(e -> e.getType() == ItemStatusEvent.Type.COMPLETED));
        } finally {
            recovery.close();
        }
    }

    // -----------------------------------------------------------------------
    // Same-caseId events land in the same partition (consistent keying)
    // -----------------------------------------------------------------------

    @Test
    @Timeout(120)
    void eventsForSameCaseId_landInSamePartition() throws Exception {
        // Pre-create iped.status with 6 partitions so a single caseId doesn't trivially
        // fill partition 0 just because that's the only partition.
        ensureStatusTopicWithPartitions(6);

        String caseA = newCaseId("partA");
        String caseB = newCaseId("partB");

        // Publish 3 events for caseA and 3 for caseB
        ItemStatusProducer producer = new ItemStatusProducer(bootstrapServers());
        try {
            for (int i = 0; i < 3; i++) {
                producer.publishDiscovered(item(caseA, 0));
                producer.publishDiscovered(item(caseB, 0));
            }
        } finally {
            producer.close();
        }
        Thread.sleep(500);

        // Consume all 6 events and group by caseId → set of partitions seen
        Map<String, Set<Integer>> partsByCaseId = new HashMap<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "partition-verify-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        try (KafkaConsumer<String, String> rawConsumer = new KafkaConsumer<>(props)) {
            rawConsumer.subscribe(List.of(ItemStatusProducer.STATUS_TOPIC));
            int seen = 0;
            while (seen < 6 && System.nanoTime() < deadline) {
                ConsumerRecords<String, String> records = rawConsumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> rec : records) {
                    String key = rec.key(); // key = caseId
                    if (caseA.equals(key) || caseB.equals(key)) {
                        partsByCaseId.computeIfAbsent(key, k -> new HashSet<>()).add(rec.partition());
                        seen++;
                    }
                }
            }
        }

        assertTrue(partsByCaseId.containsKey(caseA), "must have observed at least one caseA event");
        assertTrue(partsByCaseId.containsKey(caseB), "must have observed at least one caseB event");
        assertEquals(1, partsByCaseId.get(caseA).size(),
                "all caseA events must land in the same partition — caseId is the Kafka key so "
                        + "consistent hashing guarantees one partition per key");
        assertEquals(1, partsByCaseId.get(caseB).size(),
                "all caseB events must land in the same partition");
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

    private static KafkaItemMessage item(String caseId, int stage) {
        KafkaItemMessage m = new KafkaItemMessage();
        m.setCaseId(caseId);
        m.setItemUuid(UUID.randomUUID().toString());
        m.setPath("/evidence/" + caseId + "/item.bin");
        m.setPipelineStage(stage);
        m.setLength(256L);
        return m;
    }

    /**
     * Creates the {@code iped.status} topic with {@code partitions} partitions if it
     * doesn't already exist.  No-op if the topic already exists (the broker returns a
     * TopicExistsException which is silently swallowed).
     */
    private static void ensureStatusTopicWithPartitions(int partitions) throws Exception {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        try (AdminClient admin = AdminClient.create(p)) {
            List<NewTopic> newTopics = List.of(
                    new NewTopic(ItemStatusProducer.STATUS_TOPIC, partitions, (short) 1));
            try {
                admin.createTopics(newTopics).all().get(30, TimeUnit.SECONDS);
            } catch (Exception ex) {
                // TopicExistsException is expected when the topic was already auto-created
                // by a previous test in the same suite run; ignore it.
                if (!ex.getMessage().contains("TopicExistsException")
                        && !ex.getMessage().contains("already exists")) {
                    throw ex;
                }
            }
        }
    }
}
