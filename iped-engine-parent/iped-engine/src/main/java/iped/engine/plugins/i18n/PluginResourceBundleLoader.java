package iped.engine.plugins.i18n;


import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and manages resource bundles for plugin internationalization.
 *
 * Plugins can provide translations in multiple languages by including
 * resource bundle files in their JAR:
 *
 * plugin-xyz.jar
 * ├── com/xyz/MyComponent.class
 * └── resources/
 *     ├── iped-plugin-xyz.properties       (PT-BR, default)
 *     ├── iped-plugin-xyz_en_US.properties (English)
 *     ├── iped-plugin-xyz_es_AR.properties (Spanish)
 *     └── iped-plugin-xyz_de_DE.properties (German)
 *
 * Thread-safe for concurrent access from multiple workers.
 */
@Slf4j
public class PluginResourceBundleLoader {


    /**
     * Supported locales for IPED (in order of preference).
     */
    public static final String[] SUPPORTED_LOCALES = {
        "pt_BR",  // Portuguese (Brazil) - Default
        "en_US",  // English (United States)
        "es_AR",  // Spanish (Argentina)
        "de_DE"   // German (Germany)
    };

    /**
     * Locale string to Java Locale mapping.
     */
    private static final Map<String, Locale> LOCALE_MAP = Map.of(
        "pt_BR", new Locale("pt", "BR"),
        "en_US", new Locale("en", "US"),
        "es_AR", new Locale("es", "AR"),
        "de_DE", new Locale("de", "DE")
    );

    /**
     * Cache of loaded resource bundles.
     * Key format: "component_id|locale"
     */
    private final Map<String, ResourceBundle> bundleCache =
        new ConcurrentHashMap<>();

    /**
     * Current application locale (can be changed at runtime).
     */
    private Locale currentLocale = LOCALE_MAP.get("pt_BR");

    /**
     * Load resource bundle for a plugin component.
     * Automatically tries multiple locales in order of availability.
     *
     * @param componentId plugin component ID (e.g., "face-recognition")
     * @param classLoader classloader to load resources from
     */
    public void loadBundles(String componentId, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        for (String localeStr : SUPPORTED_LOCALES) {
            try {
                String bundleName = "iped-plugin-" + componentId;
                Locale locale = LOCALE_MAP.get(localeStr);

                ResourceBundle bundle = ResourceBundle.getBundle(
                    bundleName,
                    locale,
                    classLoader,
                    ResourceBundle.Control.getControl(
                        ResourceBundle.Control.FORMAT_PROPERTIES)
                );

                String cacheKey = getCacheKey(componentId, localeStr);
                bundleCache.put(cacheKey, bundle);

                log.debug("Loaded resource bundle for {} locale {}", componentId, localeStr);

            } catch (MissingResourceException e) {
                // OK - locale not supported by this plugin
                log.trace("No resource bundle for {} locale {}", componentId, localeStr);
            } catch (Exception e) {
                log.warn("Error loading resource bundle for {} locale {}", componentId, localeStr, e);
            }
        }
    }

    /**
     * Get a localized message for a key.
     * Uses current locale, falls back to Portuguese (default).
     *
     * @param componentId plugin component ID
     * @param key message key
     * @param args optional format arguments
     * @return localized message or key if not found
     */
    public String getMessage(String componentId, String key, Object... args) {
        return getMessage(componentId, key, currentLocale, args);
    }

    /**
     * Get a localized message for a specific locale.
     *
     * @param componentId plugin component ID
     * @param key message key
     * @param locale target locale
     * @param args optional format arguments
     * @return localized message or key if not found
     */
    public String getMessage(String componentId, String key, Locale locale, Object... args) {
        String localeStr = localeToString(locale);
        String cacheKey = getCacheKey(componentId, localeStr);

        ResourceBundle bundle = bundleCache.get(cacheKey);
        if (bundle == null) {
            // Try to fall back to Portuguese (default)
            if (!localeStr.equals("pt_BR")) {
                return getMessage(componentId, key, LOCALE_MAP.get("pt_BR"), args);
            }
            // Not found in any locale, return key
            log.trace("Message key not found: {}.{}", componentId, key);
            return key;
        }

        try {
            String message = bundle.getString(key);
            if (args.length > 0) {
                return String.format(message, args);
            }
            return message;
        } catch (MissingResourceException e) {
            log.trace("Message key not found in bundle: {}.{}", componentId, key);
            // Try fallback to Portuguese
            if (!localeStr.equals("pt_BR")) {
                return getMessage(componentId, key, LOCALE_MAP.get("pt_BR"), args);
            }
            return key;
        }
    }

    /**
     * Get a message with parameters (uses MessageFormat).
     *
     * @param componentId plugin component ID
     * @param key message key
     * @param params format parameters
     * @return formatted message
     */
    public String getFormattedMessage(String componentId, String key, Object... params) {
        String pattern = getMessage(componentId, key);
        if (pattern.equals(key)) {
            return key; // Key not found
        }

        try {
            return java.text.MessageFormat.format(pattern, params);
        } catch (Exception e) {
            log.warn("Error formatting message: {}.{}", componentId, key, e);
            return pattern;
        }
    }

    /**
     * Set the current application locale.
     * Affects all subsequent getMessage() calls without explicit locale.
     *
     * @param locale new locale to use
     */
    public void setCurrentLocale(Locale locale) {
        if (locale != null && LOCALE_MAP.containsValue(locale)) {
            this.currentLocale = locale;
            log.info("Changed application locale to: {}", locale);
        }
    }

    /**
     * Set the current application locale by locale string.
     *
     * @param localeStr locale string (e.g., "pt_BR", "en_US")
     */
    public void setCurrentLocale(String localeStr) {
        Locale locale = LOCALE_MAP.get(localeStr);
        if (locale != null) {
            setCurrentLocale(locale);
        }
    }

    /**
     * Get the current application locale.
     *
     * @return current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Get all supported locales.
     *
     * @return array of supported locale strings
     */
    public String[] getSupportedLocales() {
        return SUPPORTED_LOCALES.clone();
    }

    /**
     * Check if a locale is supported.
     *
     * @param localeStr locale string
     * @return true if supported
     */
    public boolean isSupportedLocale(String localeStr) {
        return LOCALE_MAP.containsKey(localeStr);
    }

    /**
     * Get all loaded bundles for a component.
     *
     * @param componentId plugin component ID
     * @return map of locale → resource bundle
     */
    public Map<String, ResourceBundle> getComponentBundles(String componentId) {
        Map<String, ResourceBundle> result = new HashMap<>();

        for (String localeStr : SUPPORTED_LOCALES) {
            String cacheKey = getCacheKey(componentId, localeStr);
            ResourceBundle bundle = bundleCache.get(cacheKey);
            if (bundle != null) {
                result.put(localeStr, bundle);
            }
        }

        return result;
    }

    /**
     * Clear the bundle cache (useful for testing or reloading).
     */
    public void clearCache() {
        bundleCache.clear();
        log.info("Cleared resource bundle cache");
    }

    /**
     * Get cache statistics.
     *
     * @return map of stats
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_bundles", bundleCache.size());
        stats.put("current_locale", currentLocale.toString());
        stats.put("supported_locales", SUPPORTED_LOCALES.length);
        return stats;
    }

    /**
     * Convert locale to string format (e.g., "pt_BR").
     */
    private String localeToString(Locale locale) {
        if (locale == null) {
            return "pt_BR"; // Default
        }

        String language = locale.getLanguage();
        String country = locale.getCountry();

        if (country != null && !country.isEmpty()) {
            return language + "_" + country;
        } else {
            return language;
        }
    }

    /**
     * Get cache key for bundle lookup.
     */
    private String getCacheKey(String componentId, String localeStr) {
        return componentId + "|" + localeStr;
    }

    @Override
    public String toString() {
        return "PluginResourceBundleLoader{" +
            "cached=" + bundleCache.size() +
            ", locale=" + currentLocale +
            '}';
    }
}
