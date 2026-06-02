package iped.engine.config;

import iped.configuration.Configurable;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a case-specific view of configuration by wrapping the global
 * ConfigurationManager and allowing case-specific path overrides.
 *
 * This enables multiple cases to run with different output directories,
 * temporary directories, and other case-specific configuration without
 * reloading or interfering with the global ConfigurationManager.
 */
public class ConfigurationView {

    private final ConfigurationManager globalConfigManager;
    private final ConcurrentHashMap<String, Object> overrides;

    /**
     * Create a new ConfigurationView that wraps the global ConfigurationManager.
     */
    public ConfigurationView() {
        this.globalConfigManager = ConfigurationManager.get();
        this.overrides = new ConcurrentHashMap<>();
    }

    /**
     * Find a configurable object, delegating to the global ConfigurationManager.
     *
     * @param <T> the type of configuration object
     * @param clazz the configuration class
     * @return the configuration object
     */
    public <T extends Configurable<?>> T findObject(Class<T> clazz) {
        // Check if there's an override for this type
        Object override = overrides.get(clazz.getName());
        if (override != null) {
            return clazz.cast(override);
        }

        // Delegate to global ConfigurationManager
        return globalConfigManager.findObject(clazz);
    }

    /**
     * Set a case-specific override for a configuration object.
     *
     * @param <T> the type of configuration object
     * @param clazz the configuration class
     * @param instance the configuration instance to use as override
     */
    public <T> void setOverride(Class<T> clazz, T instance) {
        overrides.put(clazz.getName(), instance);
    }

    /**
     * Remove a configuration override.
     *
     * @param clazz the configuration class
     */
    public void removeOverride(Class<?> clazz) {
        overrides.remove(clazz.getName());
    }

    /**
     * Override case-specific output directory paths.
     *
     * @param outputDir the case output directory
     * @param tempIndexDir the temporary index directory (or null to use default)
     */
    public void setOutputPaths(File outputDir, File tempIndexDir) {
        // Store these for later use by components that need case-specific paths
        overrides.put("__outputDir", outputDir);
        if (tempIndexDir != null) {
            overrides.put("__tempIndexDir", tempIndexDir);
        }
    }

    /**
     * Get the case-specific output directory if set.
     *
     * @return the output directory, or null if not set
     */
    public File getOutputDir() {
        return (File) overrides.get("__outputDir");
    }

    /**
     * Get the case-specific temporary index directory if set.
     *
     * @return the temporary index directory, or null if not set
     */
    public File getTempIndexDir() {
        return (File) overrides.get("__tempIndexDir");
    }

    /**
     * Get the global ConfigurationManager this view wraps.
     *
     * @return the global ConfigurationManager
     */
    public ConfigurationManager getGlobalConfigurationManager() {
        return globalConfigManager;
    }
}
