package iped.distributed.agent;

/**
 * Runtime configuration for a {@link TaskAgent} process, parsed from CLI arguments.
 */
public class TaskAgentConfig {

    /** IPED case identifier as registered in the Coordinator. */
    public String caseId;

    /** Simple name of the task class (e.g. {@code "HashTask"}). */
    public String taskType;

    /** Fully-qualified class name of the {@code AbstractTask} implementation. */
    public String taskClass;

    /** Kafka bootstrap servers (comma-separated). */
    public String kafkaBootstrapServers = "localhost:9092";

    /** URL of the Coordinator Server. */
    public String coordinatorUrl = "http://localhost:8484";

    /** Path to the IPED configuration directory. */
    public String configPath;

    /** Number of concurrent processing threads inside this agent. */
    public int parallelism = 4;

    /** Root path of shared storage on this node. */
    public String sharedStorageRoot = "/mnt/iped-shared";

    /** Seconds before a task execution is considered timed out. */
    public long itemTimeoutSeconds = 3600;

    /** Dead-letter queue topic suffix. */
    public String deadLetterTopicSuffix = ".dlq";

    /** Whether to use Kafka exactly-once semantics. */
    public boolean exactlyOnce = false;
}
