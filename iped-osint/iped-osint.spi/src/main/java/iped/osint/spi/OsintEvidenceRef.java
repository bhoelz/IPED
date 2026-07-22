package iped.osint.spi;

import java.util.Map;

public record OsintEvidenceRef(String label, String url, String snippet, Map<String, Object> metadata) {

    public OsintEvidenceRef {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
