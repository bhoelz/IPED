package iped.distributed.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-fast validation for {@link DistributedConfig} settings.
 *
 * <p>Callers should invoke {@link #validate} immediately after loading config and
 * refuse to start if any errors are returned.
 *
 * <pre>{@code
 * List<String> errors = ConfigValidator.validate(cfg);
 * if (!errors.isEmpty()) {
 *     errors.forEach(e -> log.error("Config error: {}", e));
 *     throw new IllegalStateException("Invalid DistributedConfig — refusing to start");
 * }
 * }</pre>
 */
public final class ConfigValidator {

    private ConfigValidator() {}

    /**
     * Validates the given config and returns a (possibly empty) list of actionable
     * error messages.  An empty list means the config is ready to use.
     */
    public static List<String> validate(DistributedConfig cfg) {
        List<String> errors = new ArrayList<>();

        // ---- Core connectivity ------------------------------------------------
        if (blank(cfg.getKafkaBootstrapServers()))
            errors.add("kafkaBootstrapServers must not be blank");

        if (blank(cfg.getCoordinatorServerUrl()))
            errors.add("coordinatorServerUrl must not be blank");

        if (blank(cfg.getSharedStorageRoot()))
            errors.add("sharedStorageRoot must not be blank");

        // ---- Port / timing ----------------------------------------------------
        int port = cfg.getCoordinatorPort();
        if (port < 1 || port > 65535)
            errors.add("coordinatorPort must be in [1, 65535], got " + port);

        if (cfg.getItemTimeoutSeconds() <= 0)
            errors.add("itemTimeoutSeconds must be positive, got " + cfg.getItemTimeoutSeconds());

        if (cfg.getAgentExpirySeconds() <= 0)
            errors.add("agentExpirySeconds must be positive, got " + cfg.getAgentExpirySeconds());

        if (cfg.getHeartbeatIntervalSeconds() <= 0)
            errors.add("heartbeatIntervalSeconds must be positive, got " + cfg.getHeartbeatIntervalSeconds());

        if (cfg.getHeartbeatIntervalSeconds() >= cfg.getAgentExpirySeconds())
            errors.add("heartbeatIntervalSeconds (" + cfg.getHeartbeatIntervalSeconds()
                    + ") must be less than agentExpirySeconds (" + cfg.getAgentExpirySeconds() + ")");

        // ---- Kafka topic sizing -----------------------------------------------
        if (cfg.getTopicPartitions() < 1)
            errors.add("topicPartitions must be >= 1, got " + cfg.getTopicPartitions());

        if (cfg.getTopicReplicationFactor() < 1)
            errors.add("topicReplicationFactor must be >= 1, got " + cfg.getTopicReplicationFactor());

        // ---- Agent / parallelism ----------------------------------------------
        if (cfg.getAgentParallelism() < 1)
            errors.add("agentParallelism must be >= 1, got " + cfg.getAgentParallelism());

        if (cfg.getMaxRetries() < 0)
            errors.add("maxRetries must be >= 0, got " + cfg.getMaxRetries());

        // ---- Work-unit sizing -------------------------------------------------
        if (cfg.getWorkUnitTargetBytes() <= 0)
            errors.add("workUnitTargetBytes must be positive, got " + cfg.getWorkUnitTargetBytes());

        if (cfg.getWorkUnitMaxItems() < 1)
            errors.add("workUnitMaxItems must be >= 1, got " + cfg.getWorkUnitMaxItems());

        if (cfg.getWorkUnitOversizedBytes() < cfg.getWorkUnitTargetBytes())
            errors.add("workUnitOversizedBytes (" + cfg.getWorkUnitOversizedBytes()
                    + ") must be >= workUnitTargetBytes (" + cfg.getWorkUnitTargetBytes() + ")");

        // ---- Backpressure thresholds ------------------------------------------
        double heapSoft = cfg.getBackpressureHeapSoftRatio();
        double heapHard = cfg.getBackpressureHeapHardRatio();

        if (heapSoft <= 0 || heapSoft >= 1)
            errors.add("backpressureHeapSoftRatio must be in (0, 1), got " + heapSoft);

        if (heapHard <= 0 || heapHard >= 1)
            errors.add("backpressureHeapHardRatio must be in (0, 1), got " + heapHard);

        if (heapSoft > 0 && heapHard > 0 && heapSoft >= heapHard)
            errors.add("backpressureHeapSoftRatio (" + heapSoft
                    + ") must be < backpressureHeapHardRatio (" + heapHard + ")");

        if (cfg.getBackpressureDiskSoftFreeBytes() <= 0)
            errors.add("backpressureDiskSoftFreeBytes must be positive, got "
                    + cfg.getBackpressureDiskSoftFreeBytes());

        if (cfg.getBackpressureDiskHardFreeBytes() <= 0)
            errors.add("backpressureDiskHardFreeBytes must be positive, got "
                    + cfg.getBackpressureDiskHardFreeBytes());

        if (cfg.getBackpressureDiskSoftFreeBytes() > 0 && cfg.getBackpressureDiskHardFreeBytes() > 0
                && cfg.getBackpressureDiskHardFreeBytes() >= cfg.getBackpressureDiskSoftFreeBytes())
            errors.add("backpressureDiskHardFreeBytes (" + cfg.getBackpressureDiskHardFreeBytes()
                    + ") must be < backpressureDiskSoftFreeBytes (" + cfg.getBackpressureDiskSoftFreeBytes() + ")");

        // ---- Security coherence ----------------------------------------------
        if (!blank(cfg.getKafkaSaslMechanism()) && blank(cfg.getKafkaSaslUsername()))
            errors.add("kafkaSaslUsername must be set when kafkaSaslMechanism is configured");

        if (!blank(cfg.getKafkaKeystorePath()) && blank(cfg.getKafkaKeystorePassword()))
            errors.add("kafkaKeystorePassword must be set when kafkaKeystorePath is configured");

        return errors;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
