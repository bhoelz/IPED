package iped.osint.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OsintResult(String executionId, String pluginId, OsintIndicatorType indicatorType, String value,
                          String normalizedValue, String fingerprint, String status, Instant startedAt,
                          Instant finishedAt, Integer itemId, String sourceId, List<OsintHit> hits,
                          Map<String, Object> audit) {

    public OsintResult {
        hits = hits == null ? List.of() : List.copyOf(hits);
        audit = audit == null ? Map.of() : Map.copyOf(audit);
    }
}
