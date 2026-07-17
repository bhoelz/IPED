package iped.datasource;

import java.util.Map;

/** Canonical relationship model shared by graph-capable additional stores. */
public record EvidenceRelationship(
    int sourceItemId, int targetItemId, String type, Map<String, Object> attributes) {
  public EvidenceRelationship {
    if (sourceItemId < 0 || targetItemId < 0)
      throw new IllegalArgumentException("item IDs must be non-negative");
    if (type == null || type.isBlank())
      throw new IllegalArgumentException("relationship type is required");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
