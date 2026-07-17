package iped.datasource;

import java.util.Map;

/** Portable per-segment output envelope; payloads remain traceable to item IDs. */
public record AdditionalStoreSegment(String caseId, String segmentId, String connectorType,
                                     Map<Integer, Map<String, Object>> itemPayloads) {
    public AdditionalStoreSegment {
        if (caseId == null || caseId.isBlank() || segmentId == null || segmentId.isBlank())
            throw new IllegalArgumentException("caseId and segmentId are required");
        if (connectorType == null || connectorType.isBlank()) throw new IllegalArgumentException("connectorType is required");
        itemPayloads = itemPayloads == null ? Map.of() : Map.copyOf(itemPayloads);
    }
}
