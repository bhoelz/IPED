package iped.distributed.agent;

import iped.io.ISeekableInputStreamFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry of {@link ISeekableInputStreamFactory} builders keyed by fully-qualified class name.
 *
 * <p>Each worker node registers builders for the factory types it is capable of reconstructing
 * (e.g. SleuthkitInputStreamFactory, AD1InputStreamFactory). During item deserialization from
 * Kafka, {@link #reconstruct} is called with the class name and parameters stored in the {@link
 * iped.distributed.kafka.KafkaItemMessage}.
 *
 * <p>Registration must happen during node startup, before any {@link
 * iped.distributed.agent.TaskAgent} begins processing.
 *
 * <p>Example registration:
 *
 * <pre>{@code
 * InputStreamFactoryRegistry.register(
 *     "iped.engine.sleuthkit.SleuthkitInputStreamFactory",
 *     params -> new SleuthkitInputStreamFactory(
 *         Paths.get(params.get("dbPath")),
 *         Long.parseLong(params.get("objectId"))
 *     )
 * );
 * }</pre>
 */
@Slf4j
public final class InputStreamFactoryRegistry {

  private static final Map<String, Function<Map<String, String>, ISeekableInputStreamFactory>>
      BUILDERS = new ConcurrentHashMap<>();

  private InputStreamFactoryRegistry() {}

  /**
   * Registers a builder for the given factory class name.
   *
   * @param fqcn fully-qualified class name of the factory
   * @param builder function that creates the factory from its serialized parameters
   */
  public static void register(
      String fqcn, Function<Map<String, String>, ISeekableInputStreamFactory> builder) {
    BUILDERS.put(fqcn, builder);
    log.info("InputStreamFactory registered for class '{}'", fqcn);
  }

  /**
   * Reconstructs a factory from its serialized form.
   *
   * @param fqcn fully-qualified class name stored in {@link
   *     iped.distributed.kafka.KafkaItemMessage}
   * @param params parameters stored in the message
   * @return a live factory capable of opening the item's content stream
   * @throws IllegalArgumentException if no builder is registered for the given class
   */
  public static ISeekableInputStreamFactory reconstruct(String fqcn, Map<String, String> params) {
    Function<Map<String, String>, ISeekableInputStreamFactory> builder = BUILDERS.get(fqcn);
    if (builder == null) {
      throw new IllegalArgumentException(
          "No InputStreamFactory builder registered for class '"
              + fqcn
              + "'. Register it via InputStreamFactoryRegistry.register() at node startup.");
    }
    return builder.apply(params);
  }

  /** Returns {@code true} if a builder is registered for the given class name. */
  public static boolean isRegistered(String fqcn) {
    return BUILDERS.containsKey(fqcn);
  }

  /** Clears all registrations. Intended for tests only. */
  public static void clearForTests() {
    BUILDERS.clear();
  }
}
