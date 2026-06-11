package iped.distributed.config;

import iped.engine.config.AbstractPropertiesConfigurable;
import iped.utils.UTF8Properties;

import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

/**
 * Configuration for IPED distributed processing mode via Apache Kafka.
 *
 * To enable distributed mode, set {@code enableDistributed = true} in
 * {@code DistributedConfig.toml} inside the IPED configuration directory.
 */
public class DistributedConfig extends AbstractPropertiesConfigurable {

    private static final long serialVersionUID = 1L;

    public static final String CONFIG_FILE = "DistributedConfig.toml";

    /** Master switch — false means local (current) behaviour, no Kafka involved. */
    private boolean enabled = false;

    /** Kafka broker list. */
    private String kafkaBootstrapServers = "localhost:9092";

    /** URL of the Coordinator REST server. */
    private String coordinatorServerUrl = "http://localhost:8484";

    /**
     * Root path on shared storage (NFS / S3 mount) that is accessible with the
     * same absolute path on every reader and task-agent node.
     */
    private String sharedStorageRoot = "/mnt/iped-shared";

    /** Number of Kafka partitions per pipeline topic. */
    private int topicPartitions = 8;

    /** Kafka replication factor for pipeline topics. */
    private short topicReplicationFactor = 1;

    /**
     * Maximum number of items processed concurrently inside a single Task Agent
     * process (= number of Worker threads per agent).
     */
    private int agentParallelism = 4;

    /** Seconds before an item/task combination is considered timed-out. */
    private long itemTimeoutSeconds = 3600;

    /** Kafka topic suffix for dead-letter queues. */
    private String deadLetterTopicSuffix = ".dlq";

    /** Failed attempts per stage before an item is sent to the DLQ (0 = no retries). */
    private int maxRetries = 3;

    /** Base delay of the exponential retry backoff: base * 2^attempt seconds. */
    private int retryBackoffBaseSeconds = 30;

    /**
     * When true, uses Kafka transactions to provide exactly-once semantics.
     * Slower but prevents duplicate processing on agent restart.
     */
    private boolean exactlyOnce = false;

    /** Port on which the Coordinator Server listens. */
    private int coordinatorPort = 8484;

    /** Seconds between agent heartbeat calls to the coordinator. */
    private int heartbeatIntervalSeconds = 10;

    /** Seconds without heartbeat after which an agent is considered dead. */
    private int agentExpirySeconds = 30;

    // -----------------------------------------------------------------------
    // AbstractPropertiesConfigurable
    // -----------------------------------------------------------------------

    @Override
    public Filter<Path> getResourceLookupFilter() {
        return entry -> entry.endsWith(CONFIG_FILE);
    }

    @Override
    public void processProperties(UTF8Properties props) {
        String v;

        v = props.getProperty("enableDistributed");
        if (v != null) enabled = Boolean.parseBoolean(v.trim());

        v = props.getProperty("kafkaBootstrapServers");
        if (v != null && !v.isBlank()) kafkaBootstrapServers = v.trim();

        v = props.getProperty("coordinatorServerUrl");
        if (v != null && !v.isBlank()) coordinatorServerUrl = v.trim();

        v = props.getProperty("sharedStorageRoot");
        if (v != null && !v.isBlank()) sharedStorageRoot = v.trim();

        v = props.getProperty("topicPartitions");
        if (v != null) topicPartitions = Integer.parseInt(v.trim());

        v = props.getProperty("topicReplicationFactor");
        if (v != null) topicReplicationFactor = Short.parseShort(v.trim());

        v = props.getProperty("agentParallelism");
        if (v != null) agentParallelism = Integer.parseInt(v.trim());

        v = props.getProperty("itemTimeoutSeconds");
        if (v != null) itemTimeoutSeconds = Long.parseLong(v.trim());

        v = props.getProperty("deadLetterTopicSuffix");
        if (v != null && !v.isBlank()) deadLetterTopicSuffix = v.trim();

        v = props.getProperty("maxRetries");
        if (v != null) maxRetries = Integer.parseInt(v.trim());

        v = props.getProperty("retryBackoffBaseSeconds");
        if (v != null) retryBackoffBaseSeconds = Integer.parseInt(v.trim());

        v = props.getProperty("exactlyOnce");
        if (v != null) exactlyOnce = Boolean.parseBoolean(v.trim());

        v = props.getProperty("coordinatorPort");
        if (v != null) coordinatorPort = Integer.parseInt(v.trim());

        v = props.getProperty("heartbeatIntervalSeconds");
        if (v != null) heartbeatIntervalSeconds = Integer.parseInt(v.trim());

        v = props.getProperty("agentExpirySeconds");
        if (v != null) agentExpirySeconds = Integer.parseInt(v.trim());
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public boolean isEnabled()                      { return enabled; }
    public String  getKafkaBootstrapServers()       { return kafkaBootstrapServers; }
    public String  getCoordinatorServerUrl()        { return coordinatorServerUrl; }
    public String  getSharedStorageRoot()           { return sharedStorageRoot; }
    public int     getTopicPartitions()             { return topicPartitions; }
    public short   getTopicReplicationFactor()      { return topicReplicationFactor; }
    public int     getAgentParallelism()            { return agentParallelism; }
    public long    getItemTimeoutSeconds()          { return itemTimeoutSeconds; }
    public String  getDeadLetterTopicSuffix()       { return deadLetterTopicSuffix; }
    public int     getMaxRetries()                  { return maxRetries; }
    public int     getRetryBackoffBaseSeconds()     { return retryBackoffBaseSeconds; }
    public boolean isExactlyOnce()                  { return exactlyOnce; }
    public int     getCoordinatorPort()             { return coordinatorPort; }
    public int     getHeartbeatIntervalSeconds()    { return heartbeatIntervalSeconds; }
    public int     getAgentExpirySeconds()          { return agentExpirySeconds; }
}
