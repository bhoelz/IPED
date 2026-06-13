package iped.distributed.it;

import iped.distributed.coordinator.AgentRegistration;
import iped.distributed.coordinator.AgentRegistry;
import iped.distributed.coordinator.AgentTopicAssigner;
import iped.distributed.coordinator.CaseCompletionMonitor;
import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.coordinator.CaseLifecycleManager.CaseStatus;
import iped.distributed.kafka.KafkaItemDeserializer;
import iped.distributed.kafka.KafkaItemMessage;
import iped.distributed.kafka.KafkaItemSerializer;
import iped.distributed.kafka.TopicProvisioner;
import iped.distributed.scheduler.CasePriority;
import iped.distributed.status.ItemStatusConsumer;
import iped.distributed.status.ItemStatusEvent;
import iped.distributed.status.ItemStatusProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
class DistributedKafkaIT {

    private static final String HASH_TASK = "HashTask";
    private static final String INDEX_TASK = "IndexTask";
    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("apache/kafka:3.8.1");

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @Test
    @Timeout(120)
    void coordinatorAssignsTopicAndKafkaItemMessageRoundTrips() throws Exception {
        String caseId = newCaseId("roundtrip");

        try (TopicProvisioner provisioner = new TopicProvisioner(bootstrapServers())) {
            CaseLifecycleManager lifecycle = new CaseLifecycleManager(provisioner);
            lifecycle.startCase(caseId, List.of(HASH_TASK), 1, (short) 1);

            AgentRegistry registry = new AgentRegistry(30);
            registry.register(AgentRegistration.of("agent-1", HASH_TASK, 0, 1, "host-1"));

            AgentTopicAssigner assigner = new AgentTopicAssigner(registry, lifecycle);
            List<String> topics = assigner.topicsForAgent("agent-1");
            assertEquals(List.of(TopicProvisioner.stageTopic(caseId, 0)), topics);

            KafkaItemMessage sent = sampleMessage(caseId, 0, true);

            try (KafkaProducer<String, KafkaItemMessage> producer = newItemProducer();
                 KafkaConsumer<String, KafkaItemMessage> consumer = newItemConsumer(
                         "roundtrip-" + UUID.randomUUID())) {
                consumer.subscribe(topics);
                awaitAssignment(consumer);

                producer.send(new ProducerRecord<>(topics.get(0), sent.getItemUuid(), sent))
                        .get(30, TimeUnit.SECONDS);

                ConsumerRecord<String, KafkaItemMessage> record = pollForRecord(consumer, topics.get(0));
                KafkaItemMessage received = record.value();

                assertEquals(sent.getItemUuid(), record.key());
                assertNotNull(received);
                assertEquals(sent.getCaseId(), received.getCaseId());
                assertEquals(sent.getItemUuid(), received.getItemUuid());
                assertEquals(sent.getPath(), received.getPath());
                assertEquals(sent.getPipelineStage(), received.getPipelineStage());
                assertEquals(sent.isPriority(), received.isPriority());
                assertEquals(sent.getWorkUnitId(), received.getWorkUnitId());
                assertEquals(sent.getWorkUnitIndex(), received.getWorkUnitIndex());
                assertEquals(sent.getLength(), received.getLength());
            }
        }
    }

    @Test
    @Timeout(120)
    void statusEventsFlowThroughKafkaAndCompleteCase() throws Exception {
        String caseId = newCaseId("status");

        try (TopicProvisioner provisioner = new TopicProvisioner(bootstrapServers())) {
            CaseLifecycleManager lifecycle = new CaseLifecycleManager(provisioner);
            lifecycle.startCase(caseId, List.of(HASH_TASK), 1, (short) 1);

            List<ItemStatusEvent> observed = new CopyOnWriteArrayList<>();
            CountDownLatch completed = new CountDownLatch(1);
            CaseCompletionMonitor monitor = new CaseCompletionMonitor(
                    lifecycle,
                    event -> {
                        observed.add(event);
                        if (event.getType() == ItemStatusEvent.Type.CASE_COMPLETED) {
                            lifecycle.completeCase(event.getCaseId());
                            completed.countDown();
                        }
                    },
                    3600);

            ItemStatusConsumer consumer = new ItemStatusConsumer(
                    bootstrapServers(),
                    "status-" + UUID.randomUUID(),
                    true,
                    monitor::onEvent);
            ItemStatusProducer producer = new ItemStatusProducer(bootstrapServers());
            try {
                consumer.start();
                Thread.sleep(1000);

                KafkaItemMessage message = sampleMessage(caseId, 0, false);
                producer.publishDiscovered(message);
                producer.publishStarted(message, HASH_TASK);
                producer.publishCompleted(message, HASH_TASK, 17L);
                producer.close();

                assertTrue(completed.await(30, TimeUnit.SECONDS), "case completion must flow through Kafka");
                assertTrue(monitor.isCompleted(caseId));
                assertEquals(CaseStatus.State.COMPLETED, lifecycle.getStatus(caseId).state);
                assertTrue(observed.stream().anyMatch(e -> e.getType() == ItemStatusEvent.Type.DISCOVERED));
                assertTrue(observed.stream().anyMatch(e -> e.getType() == ItemStatusEvent.Type.STARTED));
                assertTrue(observed.stream().anyMatch(e -> e.getType() == ItemStatusEvent.Type.COMPLETED));
                assertTrue(observed.stream().anyMatch(e -> e.getType() == ItemStatusEvent.Type.CASE_COMPLETED));
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }

    @Test
    @Timeout(120)
    void caseTopicsAreProvisionedAndDeletedByCoordinator() throws Exception {
        String caseId = newCaseId("topics");

        try (TopicProvisioner provisioner = new TopicProvisioner(bootstrapServers())) {
            CaseLifecycleManager lifecycle = new CaseLifecycleManager(provisioner);
            lifecycle.startCase(caseId, List.of(HASH_TASK, INDEX_TASK), 1, (short) 1, CasePriority.NORMAL);

            Set<String> topics = provisioner.listCaseTopics(caseId);
            assertEquals(Set.of(
                    TopicProvisioner.stageTopic(caseId, 0),
                    TopicProvisioner.stageTopic(caseId, 1),
                    TopicProvisioner.stageTopic(caseId, 2)), topics);

            assertTrue(lifecycle.deleteCase(caseId));
            assertTrue(provisioner.listCaseTopics(caseId).isEmpty());
        }
    }

    @Test
    @Timeout(120)
    void malformedPayloadIsConvertedToPoisonSentinelOnConsumption() throws Exception {
        String caseId = newCaseId("poison");
        String topic = TopicProvisioner.stageTopic(caseId, 0);

        try (TopicProvisioner provisioner = new TopicProvisioner(bootstrapServers())) {
            provisioner.provisionCase(caseId, 1, 1, (short) 1);

            try (KafkaProducer<String, byte[]> producer = newRawProducer();
                 KafkaConsumer<String, KafkaItemMessage> consumer = newPoisonConsumer(
                         "poison-" + UUID.randomUUID())) {
                consumer.subscribe(List.of(topic));
                awaitAssignment(consumer);

                producer.send(new ProducerRecord<>(topic, "poison-key",
                        "{ not valid json".getBytes(StandardCharsets.UTF_8)))
                        .get(30, TimeUnit.SECONDS);

                ConsumerRecord<String, KafkaItemMessage> record = pollForRecord(consumer, topic);
                KafkaItemMessage poison = record.value();

                assertNotNull(poison);
                assertTrue(KafkaItemDeserializer.isPoison(poison));
                assertNotNull(poison.getExtraAttributes());
                assertTrue(poison.getExtraAttributes().containsKey(KafkaItemDeserializer.POISON_ERROR_KEY));
                assertTrue(poison.getExtraAttributes().containsKey(KafkaItemDeserializer.POISON_RAW_SNIPPET_KEY));
            }
        }
    }

    private static String bootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    private static String newCaseId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static KafkaItemMessage sampleMessage(String caseId, int stage, boolean withWorkUnit) {
        KafkaItemMessage msg = new KafkaItemMessage();
        msg.setCaseId(caseId);
        msg.setItemUuid(UUID.randomUUID().toString());
        msg.setPath("/evidence/" + caseId + "/item-" + stage + ".bin");
        msg.setName("item-" + stage + ".bin");
        msg.setPipelineStage(stage);
        msg.setLength(1234L + stage);
        msg.setPriority(stage % 2 == 0);
        if (withWorkUnit) {
            msg.setWorkUnitId(caseId + ".wu.000001");
            msg.setWorkUnitIndex(2);
        }
        return msg;
    }

    private static KafkaProducer<String, KafkaItemMessage> newItemProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaItemSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        return new KafkaProducer<>(props);
    }

    private static KafkaConsumer<String, KafkaItemMessage> newItemConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaItemDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new KafkaConsumer<>(props);
    }

    private static KafkaProducer<String, byte[]> newRawProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(props);
    }

    private static KafkaConsumer<String, KafkaItemMessage> newPoisonConsumer(String groupId) {
        return newItemConsumer(groupId);
    }

    private static void awaitAssignment(KafkaConsumer<?, ?> consumer) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            consumer.poll(Duration.ofMillis(100));
            if (!consumer.assignment().isEmpty()) {
                return;
            }
        }
        fail("consumer did not receive a partition assignment in time");
    }

    private static <K, V> ConsumerRecord<K, V> pollForRecord(KafkaConsumer<K, V> consumer, String topic) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<K, V> record : records.records(topic)) {
                return record;
            }
        }
        fail("timed out waiting for a record on topic " + topic);
        return null;
    }
}
