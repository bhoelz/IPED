package iped.localization;

import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

/**
 * Two-way mapping between internal (non-localized) property names and their localized display
 * names, loaded lazily from the {@code iped-properties} bundle for the current locale.
 */
public class LocalizedProperties {

  private static final String BUNDLE_NAME = "iped-properties";

  private static final Map<String, String> map = new TreeMap<>();
  private static final Map<String, String> invertedMap = new TreeMap<>();

  private static synchronized void loadLocalizedProps() {
    if (!map.isEmpty()) {
      return;
    }
    ResourceBundle RESOURCE_BUNDLE =
        Messages.getExternalBundle(BUNDLE_NAME, LocaleResolver.getLocale());

    Enumeration<String> keys = RESOURCE_BUNDLE.getKeys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      String value = RESOURCE_BUNDLE.getString(key).trim();
      map.put(key.trim(), value);
      invertedMap.put(value, key.trim());
    }
  }

  /**
   * Maps a localized display name back to the internal property name.
   *
   * @param localizedField the localized field name
   * @return the internal name, or the input itself if no mapping exists
   */
  public static String getNonLocalizedField(String localizedField) {
    if (invertedMap.isEmpty()) {
      loadLocalizedProps();
    }
    return invertedMap.getOrDefault(localizedField, localizedField);
  }

  /**
   * Maps an internal property name to its localized display name.
   *
   * @param nonLocalizedField the internal field name
   * @return the localized name, or the input itself if no mapping exists
   */
  public static String getLocalizedField(String nonLocalizedField) {
    if (map.isEmpty()) {
      loadLocalizedProps();
    }
    return map.getOrDefault(nonLocalizedField, nonLocalizedField);
  }
}
