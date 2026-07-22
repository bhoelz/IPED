package iped.osint.spi;

import java.util.List;
import java.util.Map;

public record OsintHit(String pluginId, String source, String title, String summary, Double confidence,
                       List<OsintEvidenceRef> evidence, Map<String, Object> attributes) {

    public OsintHit {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
