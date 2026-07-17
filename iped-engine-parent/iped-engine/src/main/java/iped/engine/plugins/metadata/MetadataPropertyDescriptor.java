package iped.engine.plugins.metadata;

import java.util.Objects;

/**
 * Describes a custom metadata property that plugins can register.
 *
 * <p>Properties define what metadata can be attached to items, including: - Name and display name -
 * Data type (for serialization/deserialization) - Indexing configuration (for Elasticsearch) -
 * Component ownership (for UI grouping)
 */
public record MetadataPropertyDescriptor(
    String name,
    String displayName,
    String dataType, // "date", "boolean", "integer", "string", "decimal"
    boolean indexed, // manual: plugin declares if it should be indexed
    boolean faceted, // for Elasticsearch faceting
    String analyzer, // Elasticsearch analyzer
    String category, // ex: "plugin.social-media"
    String componentId // which component registered this property
    ) {

  public MetadataPropertyDescriptor {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(displayName, "displayName cannot be null");
    Objects.requireNonNull(dataType, "dataType cannot be null");
    Objects.requireNonNull(analyzer, "analyzer cannot be null");
    Objects.requireNonNull(category, "category cannot be null");
    Objects.requireNonNull(componentId, "componentId cannot be null");

    // Validate data type
    if (!isValidDataType(dataType)) {
      throw new IllegalArgumentException("Invalid dataType: " + dataType);
    }
  }

  /** Check if a data type is valid. */
  public static boolean isValidDataType(String type) {
    return type.equals("date")
        || type.equals("boolean")
        || type.equals("integer")
        || type.equals("string")
        || type.equals("decimal");
  }

  /** Get Elasticsearch field type for this property. */
  public String getElasticsearchFieldType() {
    return switch (dataType) {
      case "date" -> "date";
      case "boolean" -> "boolean";
      case "integer" -> "integer";
      case "decimal" -> "double";
      case "string" -> "text";
      default -> "keyword";
    };
  }

  /** Get Elasticsearch field configuration as JSON. */
  public String getElasticsearchFieldConfig() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"type\":\"").append(getElasticsearchFieldType()).append("\"");

    if (!analyzer.equals("none") && !analyzer.isEmpty()) {
      sb.append(",\"analyzer\":\"").append(analyzer).append("\"");
    }

    if (faceted && dataType.equals("string")) {
      sb.append(",\"fields\":{\"raw\":{\"type\":\"keyword\"}}");
    }

    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MetadataPropertyDescriptor)) {
      return false;
    }
    MetadataPropertyDescriptor other = (MetadataPropertyDescriptor) o;
    // Properties are equal if they have the same name
    return this.name.equals(other.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "MetadataPropertyDescriptor{"
        + "name='"
        + name
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + ", dataType='"
        + dataType
        + '\''
        + ", indexed="
        + indexed
        + ", componentId='"
        + componentId
        + '\''
        + '}';
  }
}
