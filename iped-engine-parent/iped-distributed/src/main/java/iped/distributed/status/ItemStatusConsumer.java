package iped.distributed.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Consumes {@link ItemStatusEvent}s from the global {@code iped.status} topic and
 * dispatches them to a listener. Counterpart of {@link ItemStatusProducer}.
 *
 * <p>Runs its own daemon poll thread after {@link #start()}. Offsets are not
 * committed when {@code fromBeginning} is true so every restart rebuilds the full
 * view — the listener must therefore be idempotent.
 */
@Slf4j
public class ItemStatusConsumer implements AutoCloseable {

    private final KafkaConsumer<String, String> consumer;
    private final Consumer<ItemStatusEvent> listener;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private volatile boolean running;
    private Thread pollThread;

    /**
     * @param bootstrapServers Kafka broker list
     * @param groupId          consumer group; use a unique id with {@code fromBeginning}
     *                         to rebuild full state, or a stable id to share progress
     * @param fromBeginning    when true, reads the topic from the earliest offset and
     *                         never commits, so state is rebuilt on every restart
     * @param listener         invoked on the poll thread for every parsed event
     */
    public ItemStatusConsumer(String bootstrapServers, String groupId,
                              boolean fromBeginning, Consumer<ItemStatusEvent> listener) {
        this.listener = listener;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, fromBeginning ? "earliest" : "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, !fromBeginning);
        this.consumer = new KafkaConsumer<>(props);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        pollThread = new Thread(this::pollLoop, "iped-status-consumer");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void pollLoop() {
        try {
            consumer.subscribe(List.of(ItemStatusProducer.STATUS_TOPIC));
            while (running) {
                var records = consumer.poll(Duration.ofSeconds(1));
                records.forEach(r -> dispatch(r.value()));
            }
        } catch (WakeupException e) {
            // expected on close()
        } catch (Exception e) {
            log.error("Status consumer terminated unexpectedly", e);
        } finally {
            consumer.close();
        }
    }

    private void dispatch(String json) {
        ItemStatusEvent event;
        try {
            event = mapper.readValue(json, ItemStatusEvent.class);
        } catch (Exception e) {
            log.debug("Skipping unparseable status event: {}", e.getMessage());
            return;
        }
        validateSchemaVersion(event);
        try {
            listener.accept(event);
        } catch (Exception e) {
            log.warn("Status listener failed for event type={} item={}",
                    event.getType(), event.getItemUuid(), e);
        }
    }

    /**
     * Checks the schema version on an incoming event and logs an advisory if the
     * event was produced by a different version of the codebase.
     *
     * <p>Policy:
     * <ul>
     *   <li>{@code 0} — legacy pre-versioned event; acceptable, treated as v1.</li>
     *   <li>{@code == SCHEMA_VERSION} — current; no action.</li>
     *   <li>{@code > SCHEMA_VERSION} — produced by a newer agent; forward-compatible
     *       because {@code @JsonIgnoreProperties(ignoreUnknown=true)} is in effect;
     *       warn once so operators know an upgrade may be in progress.</li>
     * </ul>
     */
    public static void validateSchemaVersion(ItemStatusEvent event) {
        int v = event.getSchemaVersion();
        if (v == 0) {
            log.debug("Received legacy (pre-v1) status event type={} item={}; "
                    + "treating as schema v1 (no known breaking changes)",
                    event.getType(), event.getItemUuid());
        } else if (v > ItemStatusEvent.SCHEMA_VERSION) {
            log.warn("Received status event with schema version {} (current: {}); "
                    + "unknown fields are silently dropped — consider upgrading this coordinator",
                    v, ItemStatusEvent.SCHEMA_VERSION);
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        consumer.wakeup();
        if (pollThread != null) {
            try {
                pollThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
