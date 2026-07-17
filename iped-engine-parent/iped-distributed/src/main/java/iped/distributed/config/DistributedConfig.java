package iped.distributed.config;

import iped.engine.config.AbstractPropertiesConfigurable;
import iped.utils.UTF8Properties;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

/**
 * Configuration for IPED distributed processing mode via Apache Kafka.
 *
 * <p>To enable distributed mode, set {@code enableDistributed = true} in {@code
 * DistributedConfig.toml} inside the IPED configuration directory.
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
   * Root path on shared storage (NFS / S3 mount) that is accessible with the same absolute path on
   * every reader and task-agent node.
   */
  private String sharedStorageRoot = "/mnt/iped-shared";

  /** Number of Kafka partitions per pipeline topic. */
  private int topicPartitions = 8;

  /** Kafka replication factor for pipeline topics. */
  private short topicReplicationFactor = 1;

  /**
   * Maximum number of items processed concurrently inside a single Task Agent process (= number of
   * Worker threads per agent).
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
   * When true, uses Kafka transactions to provide exactly-once semantics. Slower but prevents
   * duplicate processing on agent restart.
   */
  private boolean exactlyOnce = false;

  /** Port on which the Coordinator Server listens. */
  private int coordinatorPort = 8484;

  /** Seconds between agent heartbeat calls to the coordinator. */
  private int heartbeatIntervalSeconds = 10;

  /** Seconds without heartbeat after which an agent is considered dead. */
  private int agentExpirySeconds = 30;

  /**
   * Directory in which the coordinator persists case definitions for crash recovery ({@code
   * cases.json}). Empty/blank disables persistence (in-memory only). Should point to durable
   * storage that survives a coordinator restart (a mounted volume in containerised deployments).
   */
  private String coordinatorStateDir = "";

  /**
   * Adaptive work-unit sizing — soft target for the weighted byte size of one work unit (items
   * below this accumulate together). See {@code iped.distributed.workunit}.
   */
  private long workUnitTargetBytes = 64L * 1024 * 1024;

  /** Adaptive work-unit sizing — hard cap on the number of items in a single work unit. */
  private int workUnitMaxItems = 256;

  /**
   * Adaptive work-unit sizing — a single item whose weighted size reaches this is isolated into its
   * own work unit. Must be ≥ {@code workUnitTargetBytes}.
   */
  private long workUnitOversizedBytes = 128L * 1024 * 1024;

  // ---- DLQ auto-retry -----------------------------------------------------

  /**
   * When > 0, the coordinator automatically requeues DLQ items whose {@code attempt} count is below
   * this threshold on every sweep cycle. {@code 0} (default) disables automatic DLQ requeue —
   * operators drive requeue manually via the REST API.
   */
  private int dlqAutoRetryMaxAttempts = 0;

  /**
   * Milliseconds of cool-down before a DLQ auto-requeue item is eligible for agent pickup. Sets
   * {@code notBeforeMs = now + dlqAutoRetryDelayMs} on the requeued message. {@code 0} = immediate.
   */
  private long dlqAutoRetryDelayMs = 60_000L;

  /**
   * How often (in seconds) the coordinator's DLQ auto-retry sweep runs. Only effective when {@code
   * dlqAutoRetryMaxAttempts > 0}.
   */
  private int dlqAutoRetryIntervalSeconds = 120;

  /**
   * Maximum number of cases a single multi-case agent may be subscribed to at once. The
   * coordinator's heartbeat response caps the topic list to the highest-priority running cases (by
   * {@link iped.distributed.scheduler.CasePriority}, then alphabetical). {@code 0} (default) =
   * unlimited.
   */
  private int maxSubscribedCases = 0;

  // ---- Backpressure (agent resource pressure) ----------------------------

  /** Heap used-ratio at/above which an agent reports SOFT backpressure. */
  private double backpressureHeapSoftRatio = 0.75;

  /** Heap used-ratio at/above which an agent reports HARD backpressure (no new work). */
  private double backpressureHeapHardRatio = 0.90;

  /** Free disk (bytes) on the work volume at/below which an agent reports SOFT backpressure. */
  private long backpressureDiskSoftFreeBytes = 10L * 1024 * 1024 * 1024;

  /** Free disk (bytes) on the work volume at/below which an agent reports HARD backpressure. */
  private long backpressureDiskHardFreeBytes = 5L * 1024 * 1024 * 1024;

  // ---- Dual-run mode (strangler pattern) ---------------------------------

  /**
   * When true, the coordinator automatically opens a {@code DualRunSession} for every new case and
   * exposes the {@code /api/v1/dualrun/{caseId}} and {@code /api/v1/dualrun/{caseId}/reference}
   * endpoints. A dual-run session can also be started manually via {@code POST
   * /api/v1/dualrun/{caseId}/start} regardless of this flag.
   */
  private boolean dualRunEnabled = false;

  // ---- Kafka security (TLS / SASL) ----------------------------------------

  /** When true, Kafka clients use SSL/TLS for transport encryption. */
  private boolean kafkaTlsEnabled = false;

  /** Path to the JKS or PKCS12 truststore used to verify the Kafka broker certificate. */
  private String kafkaTruststorePath = "";

  /** Password for the truststore. */
  private String kafkaTruststorePassword = "";

  /** Path to the JKS or PKCS12 keystore for mTLS client authentication (optional). */
  private String kafkaKeystorePath = "";

  /** Password for the keystore. */
  private String kafkaKeystorePassword = "";

  /** Password for the private key inside the keystore. */
  private String kafkaKeyPassword = "";

  /**
   * SASL mechanism for broker authentication. Supported values: {@code PLAIN}, {@code
   * SCRAM-SHA-256}, {@code SCRAM-SHA-512}. Empty/blank = SASL disabled. Combine with {@code
   * kafkaTlsEnabled=true} for encrypted credentials (SASL_SSL).
   */
  private String kafkaSaslMechanism = "";

  /** Username for SASL authentication. */
  private String kafkaSaslUsername = "";

  /** Password for SASL authentication. */
  private String kafkaSaslPassword = "";

  // ---- Payload signing ----------------------------------------------------

  /**
   * Shared HMAC-SHA256 secret for signing/verifying Kafka work-unit payloads. Empty/blank = payload
   * signing disabled. All agents in a cluster must share the same secret. Use a cryptographically
   * random string of ≥ 32 characters.
   */
  private String payloadSigningSecret = "";

  // ---- Audit log (chain-of-custody) ----------------------------------------

  /**
   * Path to the CSV file where processing outcomes are durably logged for chain-of-custody
   * purposes. Empty/blank = in-memory only (lost on restart). The file is opened in append mode;
   * existing records are preserved across restarts.
   */
  private String auditLogPath = "";

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

    v = props.getProperty("coordinatorStateDir");
    if (v != null) coordinatorStateDir = v.trim();

    v = props.getProperty("workUnitTargetBytes");
    if (v != null) workUnitTargetBytes = Long.parseLong(v.trim());

    v = props.getProperty("workUnitMaxItems");
    if (v != null) workUnitMaxItems = Integer.parseInt(v.trim());

    v = props.getProperty("workUnitOversizedBytes");
    if (v != null) workUnitOversizedBytes = Long.parseLong(v.trim());

    v = props.getProperty("dlqAutoRetryMaxAttempts");
    if (v != null) dlqAutoRetryMaxAttempts = Integer.parseInt(v.trim());
    v = props.getProperty("dlqAutoRetryDelayMs");
    if (v != null) dlqAutoRetryDelayMs = Long.parseLong(v.trim());
    v = props.getProperty("dlqAutoRetryIntervalSeconds");
    if (v != null) dlqAutoRetryIntervalSeconds = Integer.parseInt(v.trim());

    v = props.getProperty("maxSubscribedCases");
    if (v != null) maxSubscribedCases = Integer.parseInt(v.trim());

    v = props.getProperty("backpressureHeapSoftRatio");
    if (v != null) backpressureHeapSoftRatio = Double.parseDouble(v.trim());

    v = props.getProperty("backpressureHeapHardRatio");
    if (v != null) backpressureHeapHardRatio = Double.parseDouble(v.trim());

    v = props.getProperty("backpressureDiskSoftFreeBytes");
    if (v != null) backpressureDiskSoftFreeBytes = Long.parseLong(v.trim());

    v = props.getProperty("backpressureDiskHardFreeBytes");
    if (v != null) backpressureDiskHardFreeBytes = Long.parseLong(v.trim());

    v = props.getProperty("dualRunEnabled");
    if (v != null) dualRunEnabled = Boolean.parseBoolean(v.trim());

    v = props.getProperty("kafkaTlsEnabled");
    if (v != null) kafkaTlsEnabled = Boolean.parseBoolean(v.trim());
    v = props.getProperty("kafkaTruststorePath");
    if (v != null) kafkaTruststorePath = v.trim();
    v = props.getProperty("kafkaTruststorePassword");
    if (v != null) kafkaTruststorePassword = v.trim();
    v = props.getProperty("kafkaKeystorePath");
    if (v != null) kafkaKeystorePath = v.trim();
    v = props.getProperty("kafkaKeystorePassword");
    if (v != null) kafkaKeystorePassword = v.trim();
    v = props.getProperty("kafkaKeyPassword");
    if (v != null) kafkaKeyPassword = v.trim();
    v = props.getProperty("kafkaSaslMechanism");
    if (v != null) kafkaSaslMechanism = v.trim();
    v = props.getProperty("kafkaSaslUsername");
    if (v != null) kafkaSaslUsername = v.trim();
    v = props.getProperty("kafkaSaslPassword");
    if (v != null) kafkaSaslPassword = v.trim();
    v = props.getProperty("payloadSigningSecret");
    if (v != null) payloadSigningSecret = v.trim();
    v = props.getProperty("auditLogPath");
    if (v != null) auditLogPath = v.trim();
  }

  // -----------------------------------------------------------------------
  // Getters
  // -----------------------------------------------------------------------

  public boolean isEnabled() {
    return enabled;
  }

  public String getKafkaBootstrapServers() {
    return kafkaBootstrapServers;
  }

  public String getCoordinatorServerUrl() {
    return coordinatorServerUrl;
  }

  public String getSharedStorageRoot() {
    return sharedStorageRoot;
  }

  public int getTopicPartitions() {
    return topicPartitions;
  }

  public short getTopicReplicationFactor() {
    return topicReplicationFactor;
  }

  public int getAgentParallelism() {
    return agentParallelism;
  }

  public long getItemTimeoutSeconds() {
    return itemTimeoutSeconds;
  }

  public String getDeadLetterTopicSuffix() {
    return deadLetterTopicSuffix;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public int getRetryBackoffBaseSeconds() {
    return retryBackoffBaseSeconds;
  }

  public boolean isExactlyOnce() {
    return exactlyOnce;
  }

  public int getCoordinatorPort() {
    return coordinatorPort;
  }

  public int getHeartbeatIntervalSeconds() {
    return heartbeatIntervalSeconds;
  }

  public int getAgentExpirySeconds() {
    return agentExpirySeconds;
  }

  public String getCoordinatorStateDir() {
    return coordinatorStateDir;
  }

  public long getWorkUnitTargetBytes() {
    return workUnitTargetBytes;
  }

  public int getWorkUnitMaxItems() {
    return workUnitMaxItems;
  }

  public long getWorkUnitOversizedBytes() {
    return workUnitOversizedBytes;
  }

  public int getDlqAutoRetryMaxAttempts() {
    return dlqAutoRetryMaxAttempts;
  }

  public long getDlqAutoRetryDelayMs() {
    return dlqAutoRetryDelayMs;
  }

  public int getDlqAutoRetryIntervalSeconds() {
    return dlqAutoRetryIntervalSeconds;
  }

  public int getMaxSubscribedCases() {
    return maxSubscribedCases;
  }

  public double getBackpressureHeapSoftRatio() {
    return backpressureHeapSoftRatio;
  }

  public double getBackpressureHeapHardRatio() {
    return backpressureHeapHardRatio;
  }

  public long getBackpressureDiskSoftFreeBytes() {
    return backpressureDiskSoftFreeBytes;
  }

  public long getBackpressureDiskHardFreeBytes() {
    return backpressureDiskHardFreeBytes;
  }

  public boolean isDualRunEnabled() {
    return dualRunEnabled;
  }

  public boolean isKafkaTlsEnabled() {
    return kafkaTlsEnabled;
  }

  public String getKafkaTruststorePath() {
    return kafkaTruststorePath;
  }

  public String getKafkaTruststorePassword() {
    return kafkaTruststorePassword;
  }

  public String getKafkaKeystorePath() {
    return kafkaKeystorePath;
  }

  public String getKafkaKeystorePassword() {
    return kafkaKeystorePassword;
  }

  public String getKafkaKeyPassword() {
    return kafkaKeyPassword;
  }

  public String getKafkaSaslMechanism() {
    return kafkaSaslMechanism;
  }

  public String getKafkaSaslUsername() {
    return kafkaSaslUsername;
  }

  public String getKafkaSaslPassword() {
    return kafkaSaslPassword;
  }

  public String getPayloadSigningSecret() {
    return payloadSigningSecret;
  }

  public String getAuditLogPath() {
    return auditLogPath;
  }
}
