package iped.distributed.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Kafka Deserializer for {@link KafkaItemMessage} — reads from UTF-8 JSON bytes.
 *
 * <h2>Poison-message handling</h2>
 *
 * <p>This deserializer <em>never throws</em>. If the byte payload cannot be parsed (malformed JSON,
 * incompatible schema, truncated data …) it returns a synthetic {@link KafkaItemMessage} sentinel
 * rather than propagating an exception. Throwing from inside {@code deserialize()} would crash the
 * {@code consumer.poll()} call in {@link iped.distributed.agent.TaskAgent}, stalling the partition
 * permanently.
 *
 * <p>The sentinel is detected by {@link #isPoison(KafkaItemMessage)} and carries diagnostic
 * information in its {@code extraAttributes} map:
 *
 * <ul>
 *   <li>{@value #POISON_ERROR_KEY} — exception message from the failed parse.
 *   <li>{@value #POISON_RAW_SNIPPET_KEY} — hex-encoded prefix of the raw bytes (up to {@value
 *       #RAW_SNIPPET_MAX_BYTES} bytes) to aid operator investigation.
 * </ul>
 *
 * {@link iped.distributed.agent.TaskAgent} detects the sentinel early in {@code processRecord} and
 * routes it directly to the stage's dead-letter queue without invoking any task logic, then commits
 * the offset so the partition continues.
 */
@Slf4j
public class KafkaItemDeserializer implements Deserializer<KafkaItemMessage> {

  /** {@code extraAttributes} key present on every poison-message sentinel. */
  public static final String POISON_ERROR_KEY = "__poison.error";

  /** {@code extraAttributes} key carrying a hex snippet of the raw bytes. */
  public static final String POISON_RAW_SNIPPET_KEY = "__poison.rawSnippet";

  /** Number of raw bytes preserved in the hex snippet. */
  public static final int RAW_SNIPPET_MAX_BYTES = 100;

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @Override
  public KafkaItemMessage deserialize(String topic, byte[] data) {
    if (data == null) return null;
    try {
      return MAPPER.readValue(data, KafkaItemMessage.class);
    } catch (Exception e) {
      log.error(
          "Poison message on topic '{}' — deserialization failed: {}; "
              + "routing to DLQ instead of stalling the partition",
          topic,
          e.getMessage());
      return buildSentinel(topic, data, e);
    }
  }

  // -----------------------------------------------------------------------
  // Poison detection — used by TaskAgent to skip task execution
  // -----------------------------------------------------------------------

  /**
   * Returns {@code true} if {@code msg} is a poison-message sentinel produced by this deserializer,
   * i.e. the original Kafka record could not be parsed.
   */
  public static boolean isPoison(KafkaItemMessage msg) {
    return msg != null
        && msg.getExtraAttributes() != null
        && msg.getExtraAttributes().containsKey(POISON_ERROR_KEY);
  }

  // -----------------------------------------------------------------------
  // Internal
  // -----------------------------------------------------------------------

  private static KafkaItemMessage buildSentinel(String topic, byte[] data, Exception cause) {
    KafkaItemMessage sentinel = new KafkaItemMessage();
    sentinel.setItemUuid(UUID.randomUUID().toString());
    sentinel.setPath("(undeserializable message on topic " + topic + ")");
    sentinel.setPipelineStage(stageFromTopic(topic));

    Map<String, Object> attrs = new LinkedHashMap<>();
    attrs.put(
        POISON_ERROR_KEY,
        cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName());
    attrs.put(POISON_RAW_SNIPPET_KEY, hexSnippet(data));
    sentinel.setExtraAttributes(attrs);
    return sentinel;
  }

  /** Extracts the pipeline stage number from a topic name such as {@code iped.case1.stage.3}. */
  private static int stageFromTopic(String topic) {
    try {
      int idx = topic.lastIndexOf('.');
      if (idx >= 0) return Integer.parseInt(topic.substring(idx + 1));
    } catch (NumberFormatException ignored) {
    }
    return -1;
  }

  private static String hexSnippet(byte[] data) {
    int len = Math.min(data.length, RAW_SNIPPET_MAX_BYTES);
    return HexFormat.of().formatHex(data, 0, len)
        + (data.length > RAW_SNIPPET_MAX_BYTES ? "…(" + data.length + " bytes total)" : "");
  }
}
