package iped.osint.spi;

import java.util.Map;
import java.util.Set;

public record OsintPluginDescriptor(String id, String displayName, Set<OsintIndicatorType> supportedIndicatorTypes,
                                    Set<OsintExecutionMode> supportedExecutionModes, Set<OsintCapability> capabilities,
                                    String description, Map<String, Object> configSchema, int defaultTimeoutSeconds,
                                    int maxBatchSize) {

    public OsintPluginDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Plugin id must not be blank");
        }
        displayName = (displayName == null || displayName.isBlank()) ? id : displayName;
        supportedIndicatorTypes = supportedIndicatorTypes == null ? Set.of() : Set.copyOf(supportedIndicatorTypes);
        supportedExecutionModes = supportedExecutionModes == null ? Set.of() : Set.copyOf(supportedExecutionModes);
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        configSchema = configSchema == null ? Map.of() : Map.copyOf(configSchema);
        defaultTimeoutSeconds = defaultTimeoutSeconds <= 0 ? 30 : defaultTimeoutSeconds;
        maxBatchSize = maxBatchSize <= 0 ? 1 : maxBatchSize;
    }
}
