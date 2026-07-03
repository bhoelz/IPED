package iped.osint.spi;

import java.util.Map;

public record OsintQuery(String pluginId, OsintIndicatorType indicatorType, String value, String normalizedValue,
                         Integer itemId, String sourceId, OsintExecutionMode mode, Map<String, Object> options) {

    public OsintQuery {
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
