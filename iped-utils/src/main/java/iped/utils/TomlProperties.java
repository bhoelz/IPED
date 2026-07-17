package iped.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Loads TOML configuration files into a flat {@link UTF8Properties} view, so existing property
 * based configuration code keeps working unchanged.
 *
 * <p>Flattening rules:
 *
 * <ul>
 *   <li>scalars keep their TOML key and are stored as strings (booleans/numbers in canonical form,
 *       e.g. "true", "1073741824");
 *   <li>arrays of scalars are joined with "; ", matching the project's legacy semicolon separated
 *       list convention;
 *   <li>nested tables are flattened with dotted keys ("table.key").
 * </ul>
 *
 * Loading multiple files into the same instance merges them with key level last-wins semantics,
 * which is what enables deviation-only configuration layers on top of built-in defaults.
 */
public class TomlProperties extends UTF8Properties {

  private static final long serialVersionUID = 1L;

  public static final String LIST_SEPARATOR = "; ";

  private static final TomlMapper MAPPER = new TomlMapper();

  public synchronized void load(Path path) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      load(is);
    }
  }

  public synchronized void load(InputStream is) throws IOException {
    JsonNode root = MAPPER.readTree(is);
    if (root != null && root.isObject()) {
      flatten("", root); // $NON-NLS-1$
    }
  }

  private void flatten(String prefix, JsonNode node) throws IOException {
    for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey(); // $NON-NLS-1$
      JsonNode value = entry.getValue();
      if (value.isObject()) {
        flatten(key, value);
      } else if (value.isArray()) {
        super.put(key, joinArray(key, value));
      } else {
        super.put(key, value.asText());
      }
    }
  }

  private String joinArray(String key, JsonNode array) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (JsonNode element : array) {
      if (element.isObject() || element.isArray()) {
        throw new IOException(
            "Unsupported nested structure in TOML array '"
                + key //$NON-NLS-1$
                + "': only arrays of scalars can be flattened to properties"); //$NON-NLS-1$
      }
      if (sb.length() > 0) {
        sb.append(LIST_SEPARATOR);
      }
      sb.append(element.asText());
    }
    return sb.toString();
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = getProperty(key);
    return value != null && !value.isBlank() ? Boolean.parseBoolean(value.trim()) : defaultValue;
  }

  public int getIntProperty(String key, int defaultValue) {
    String value = getProperty(key);
    return value != null && !value.isBlank() ? Integer.parseInt(value.trim()) : defaultValue;
  }

  public long getLongProperty(String key, long defaultValue) {
    String value = getProperty(key);
    return value != null && !value.isBlank() ? Long.parseLong(value.trim()) : defaultValue;
  }

  public double getDoubleProperty(String key, double defaultValue) {
    String value = getProperty(key);
    return value != null && !value.isBlank() ? Double.parseDouble(value.trim()) : defaultValue;
  }

  /**
   * Returns a list property, accepting both flattened TOML arrays and legacy semicolon separated
   * strings.
   */
  public List<String> getListProperty(String key) {
    List<String> result = new ArrayList<>();
    String value = getProperty(key);
    if (value != null) {
      for (String item : value.split(";")) { // $NON-NLS-1$
        item = item.trim();
        if (!item.isEmpty()) {
          result.add(item);
        }
      }
    }
    return result;
  }

  /**
   * Writes the given key/value map as a flat TOML document. Used by tooling that persists
   * deviation-only configuration files.
   */
  public static void store(Map<String, Object> values, Path path) throws IOException {
    ObjectNode root = MAPPER.createObjectNode();
    values.forEach((k, v) -> root.putPOJO(k, v));
    try (var os = Files.newOutputStream(path)) {
      MAPPER.writeValue(os, root);
    }
  }
}
