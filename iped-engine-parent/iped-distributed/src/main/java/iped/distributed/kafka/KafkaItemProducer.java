package iped.distributed.kafka;

import iped.data.ICaseData;
import iped.data.IItem;
import iped.distributed.config.DistributedConfig;
import iped.distributed.status.ItemStatusProducer;
import iped.distributed.workunit.AdaptiveWorkUnitPlanner;
import iped.distributed.workunit.WorkUnit;
import iped.engine.datasource.IDatasourceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Distributed implementation of {@link IDatasourceRegistry}.
 *
 * <p>Replaces the local in-memory {@code ProcessingQueues} with a Kafka producer.
 * Items created by datasource readers are serialized to {@link KafkaItemMessage}
 * and published to the case's stage-0 topic, from which Task Agents pick them up.
 *
 * <p>All other {@link IDatasourceRegistry} operations (item creation, track-ID
 * computation, env-vars, progress) are delegated to a local {@code Manager}
 * instance via the {@code localDelegate} — so the engine can still manage item
 * IDs, track IDs, and configuration locally.
 *
 * <p>This class is <em>thread-safe</em>: multiple reader threads may call
 * {@link #addItem} concurrently.
 */
@Slf4j
public class KafkaItemProducer implements IDatasourceRegistry, AutoCloseable {


    private final String                          caseId;
    private final String                          rawTopic;     // stage.0
    private final KafkaProducer<String, KafkaItemMessage> kafkaProducer;
    private final ItemStatusProducer              statusProducer;
    private final IDatasourceRegistry             localDelegate;

    // ---- Work-unit buffering (guarded by this) ------------------------------
    private final AdaptiveWorkUnitPlanner         planner;
    /** Buffer of not-yet-published messages in input order. */
    private final List<KafkaItemMessage>          buffer    = new ArrayList<>();
    /** UUID-to-message index so we can look up after planning. */
    private final Map<String, KafkaItemMessage>   bufferMap = new LinkedHashMap<>();
    private int                                   unitSeq   = 0; // monotone work-unit counter

    public KafkaItemProducer(DistributedConfig cfg,
                             String caseId,
                             IDatasourceRegistry localDelegate) {
        this(cfg, caseId, localDelegate, new AdaptiveWorkUnitPlanner(
                iped.distributed.workunit.WorkUnitSizingPolicy.fromConfig(cfg),
                new iped.distributed.workunit.MediaCostModel()));
    }

    public KafkaItemProducer(DistributedConfig cfg,
                             String caseId,
                             IDatasourceRegistry localDelegate,
                             AdaptiveWorkUnitPlanner planner) {
        this.caseId        = caseId;
        this.rawTopic      = TopicProvisioner.stageTopic(caseId, 0);
        this.localDelegate = localDelegate;
        this.planner       = planner;
        this.statusProducer = new ItemStatusProducer(cfg.getKafkaBootstrapServers());

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getKafkaBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                  KafkaItemSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");          // durable writes
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 64 * 1024);
        if (cfg.isExactlyOnce()) {
            props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "iped-reader-" + caseId);
        }
        this.kafkaProducer = new KafkaProducer<>(props);

        if (cfg.isExactlyOnce()) {
            kafkaProducer.initTransactions();
        }

        log.info("KafkaItemProducer initialised for case '{}', publishing to '{}'",
                    caseId, rawTopic);
    }

    // -----------------------------------------------------------------------
    // IDatasourceRegistry — item queue operations (Kafka path)
    // -----------------------------------------------------------------------

    @Override
    public void addItem(IItem item) throws InterruptedException {
        publish(item, false);
    }

    @Override
    public void addItemFirst(IItem item) throws InterruptedException {
        // Mark as priority so Task Agents can dequeue it ahead of regular items
        publish(item, true);
    }

    private synchronized void publish(IItem item, boolean priority) {
        KafkaItemMessage msg = ItemConverter.toMessage(item, caseId);
        msg.setPriority(priority);
        buffer.add(msg);
        bufferMap.put(msg.getItemUuid(), msg);
        flushCompletedUnits(false);
    }

    /**
     * Plans the current buffer and publishes all completed work units.
     * When {@code flushAll} is {@code true}, publishes the final (possibly partial) unit too.
     */
    private void flushCompletedUnits(boolean flushAll) {
        if (buffer.isEmpty()) return;
        List<WorkUnit> units = planner.plan(buffer);
        int toPublish = flushAll ? units.size() : units.size() - 1;
        if (toPublish <= 0) return;

        for (int u = 0; u < toPublish; u++) {
            WorkUnit unit = units.get(u);
            String workUnitId = String.format("%s.wu.%06d", caseId, unitSeq++);
            for (int i = 0; i < unit.itemUuids().size(); i++) {
                KafkaItemMessage msg = bufferMap.get(unit.itemUuids().get(i));
                if (msg == null) continue;
                msg.setWorkUnitId(workUnitId);
                msg.setWorkUnitIndex(i);
                sendToKafka(msg);
            }
        }
        // Remove published items from the buffer
        int published = units.subList(0, toPublish).stream()
                .mapToInt(WorkUnit::itemCount).sum();
        for (int i = 0; i < published; i++) {
            KafkaItemMessage removed = buffer.remove(0);
            bufferMap.remove(removed.getItemUuid());
        }
    }

    private void sendToKafka(KafkaItemMessage msg) {
        ProducerRecord<String, KafkaItemMessage> record =
                new ProducerRecord<>(rawTopic, msg.getItemUuid(), msg);
        kafkaProducer.send(record, (metadata, ex) -> {
            if (ex != null) {
                log.error("Failed to publish item '{}' (path='{}') to Kafka topic '{}'",
                             msg.getItemUuid(), msg.getPath(), rawTopic, ex);
            } else {
                log.debug("Published item '{}' (wu={},{}) → {}:{}", msg.getItemUuid(),
                             msg.getWorkUnitId(), msg.getWorkUnitIndex(),
                             metadata.topic(), metadata.offset());
            }
        });
        statusProducer.publishDiscovered(msg);
    }

    // -----------------------------------------------------------------------
    // IDatasourceRegistry — delegated to local engine
    // -----------------------------------------------------------------------

    @Override
    public IItem createItem() {
        return localDelegate.createItem();
    }

    @Override
    public void prepareForQueue(IItem item, ICaseData caseData) {
        localDelegate.prepareForQueue(item, caseData);
    }

    @Override
    public String getTrackID(IItem item) {
        return localDelegate.getTrackID(item);
    }

    @Override
    public void onProgress(String propertyName, Object oldValue, Object newValue) {
        // Forward progress as a status event; also delegate for local UI if present
        localDelegate.onProgress(propertyName, oldValue, newValue);
    }

    @Override
    public void setEnvVar(String key, String value) {
        localDelegate.setEnvVar(key, value);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public synchronized void flush() {
        flushCompletedUnits(true);   // publish remaining partial unit
        kafkaProducer.flush();
    }

    @Override
    public void close() {
        kafkaProducer.flush();
        kafkaProducer.close();
        statusProducer.close();
        log.info("KafkaItemProducer closed for case '{}'", caseId);
    }
}
