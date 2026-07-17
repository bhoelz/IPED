package iped.datasource;

import java.time.Instant;
import java.util.Map;

/** Immutable timeline event with traceability to the original evidence item. */
public record TimelineEvent(
    int itemId, Instant timestamp, String type, Map<String, Object> attributes) {
  public TimelineEvent {
    if (itemId < 0) throw new IllegalArgumentException("itemId must be non-negative");
    if (timestamp == null) throw new IllegalArgumentException("timestamp is required");
    if (type == null || type.isBlank()) throw new IllegalArgumentException("type is required");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
