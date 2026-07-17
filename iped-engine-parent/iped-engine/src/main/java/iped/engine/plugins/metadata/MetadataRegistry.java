package iped.engine.plugins.metadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Central registry for custom metadata properties.
 *
 * <p>Manages registration of custom metadata properties from plugins, with support for
 * Elasticsearch indexing configuration and type validation.
 *
 * <p>Thread-safe for concurrent access from multiple workers.
 */
@Slf4j
public class MetadataRegistry {

  /** Registry of all metadata properties by name. */
  private final Map<String, MetadataPropertyDescriptor> properties = new ConcurrentHashMap<>();

  /**
   * Register a custom metadata property.
   *
   * @param descriptor property descriptor
   */
  public void registerProperty(MetadataPropertyDescriptor descriptor) {
    if (descriptor == null) {
      throw new IllegalArgumentException("descriptor cannot be null");
    }

    String key = descriptor.name();
    MetadataPropertyDescriptor previous = properties.put(key, descriptor);

    if (previous == null) {
      log.debug("Registered metadata property: {} ({})", key, descriptor.displayName());
    } else {
      log.debug("Updated metadata property: {} ({})", key, descriptor.displayName());
    }
  }

  /**
   * Get a metadata property by name.
   *
   * @param name property name
   * @return descriptor or null if not found
   */
  public MetadataPropertyDescriptor getProperty(String name) {
    return properties.get(name);
  }

  /**
   * Get all registered properties.
   *
   * @return list of all property descriptors
   */
  public List<MetadataPropertyDescriptor> getAllProperties() {
    return new ArrayList<>(properties.values());
  }

  /**
   * Get properties that are marked as indexed.
   *
   * @return list of indexed properties
   */
  public List<MetadataPropertyDescriptor> getIndexedProperties() {
    return properties.values().stream()
        .filter(MetadataPropertyDescriptor::indexed)
        .collect(Collectors.toList());
  }

  /**
   * Get properties registered by a specific component.
   *
   * @param componentId component ID
   * @return list of component's properties
   */
  public List<MetadataPropertyDescriptor> getPropertiesByComponent(String componentId) {
    return properties.values().stream()
        .filter(p -> p.componentId().equals(componentId))
        .collect(Collectors.toList());
  }

  /**
   * Get properties in a specific category.
   *
   * @param category category name
   * @return list of properties in category
   */
  public List<MetadataPropertyDescriptor> getPropertiesByCategory(String category) {
    return properties.values().stream()
        .filter(p -> p.category().equals(category))
        .collect(Collectors.toList());
  }

  /**
   * Get Elasticsearch mapping for all indexed properties.
   *
   * @return JSON mapping configuration
   */
  public String getElasticsearchMapping() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"properties\":{");

    List<MetadataPropertyDescriptor> indexed = getIndexedProperties();
    for (int i = 0; i < indexed.size(); i++) {
      MetadataPropertyDescriptor prop = indexed.get(i);
      sb.append("\"").append(prop.name()).append("\":").append(prop.getElasticsearchFieldConfig());

      if (i < indexed.size() - 1) {
        sb.append(",");
      }
    }

    sb.append("}}");
    return sb.toString();
  }

  /**
   * Get registry statistics.
   *
   * @return map of statistics
   */
  public Map<String, Object> getStatistics() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("total_properties", properties.size());
    stats.put("indexed_properties", getIndexedProperties().size());
    stats.put(
        "faceted_properties",
        (int) properties.values().stream().filter(MetadataPropertyDescriptor::faceted).count());

    Set<String> components =
        properties.values().stream()
            .map(MetadataPropertyDescriptor::componentId)
            .collect(Collectors.toSet());
    stats.put("components_with_properties", components.size());

    Set<String> categories =
        properties.values().stream()
            .map(MetadataPropertyDescriptor::category)
            .collect(Collectors.toSet());
    stats.put("unique_categories", categories.size());

    return stats;
  }

  /** Clear all registered properties. */
  public void clearRegistry() {
    properties.clear();
    log.info("Cleared metadata property registry");
  }

  /**
   * Check if a property is registered.
   *
   * @param name property name
   * @return true if registered
   */
  public boolean hasProperty(String name) {
    return properties.containsKey(name);
  }

  /**
   * Get number of registered properties.
   *
   * @return property count
   */
  public int getPropertyCount() {
    return properties.size();
  }

  @Override
  public String toString() {
    return "MetadataRegistry{"
        + "properties="
        + properties.size()
        + ", indexed="
        + getIndexedProperties().size()
        + '}';
  }
}
