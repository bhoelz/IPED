package iped.engine.config;

import iped.configuration.Configurable;
import iped.configuration.IConfigurationDirectory;
import iped.configuration.ITypedConfigAccess;
import iped.configuration.ObjectManager;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class ConfigurationManager implements ObjectManager<Configurable<?>>, ITypedConfigAccess {

    /**
     * Process-global singleton used by the legacy monolithic (single-case) code
     * path. Multi-case callers should use {@link #createCaseInstance} instead so
     * each {@code CaseContext} gets its own isolated configuration state.
     */
    private static volatile ConfigurationManager singleton = null;

    private IConfigurationDirectory directory;
    private Map<Configurable<?>, Boolean> loadedConfigurables = new LinkedHashMap<>();

    /**
     * Returns the process-global singleton. Only valid in single-case (monolithic)
     * mode. Multi-case code must not use this — use the instance stored on the
     * {@code CaseContext} instead.
     *
     * @return the singleton, or {@code null} if {@link #createInstance} was never called
     * @deprecated Use {@link #createCaseInstance(IConfigurationDirectory)} for multi-case
     *             isolation and store the result on the {@code CaseContext}.
     */
    @Deprecated
    public static ConfigurationManager get() {
        return singleton;
    }

    /**
     * Creates (or returns the existing) process-global singleton.
     * Retained for the monolithic single-case launch path.
     *
     * @deprecated Prefer {@link #createCaseInstance(IConfigurationDirectory)} for
     *             new multi-case-aware code.
     */
    @Deprecated
    public static ConfigurationManager createInstance(IConfigurationDirectory directory) {
        if (singleton == null) {
            synchronized (ConfigurationManager.class) {
                if (singleton == null) {
                    singleton = new ConfigurationManager(directory);
                }
            }
        }
        return singleton;
    }

    /**
     * Creates a fresh {@code ConfigurationManager} instance scoped to one case.
     * The instance is not registered as the process-global singleton, so each
     * {@code CaseContext} can hold its own independent configuration without
     * cross-case contamination.
     *
     * <p>Callers are responsible for storing and propagating the returned instance
     * via {@code CaseContext} (or an equivalent per-case container) rather than
     * relying on {@link #get()}.
     *
     * @param directory the configuration directory for the new case; must not be {@code null}
     * @return a new, empty {@code ConfigurationManager} bound to {@code directory}
     */
    public static ConfigurationManager createCaseInstance(IConfigurationDirectory directory) {
        return new ConfigurationManager(directory);
    }

    private ConfigurationManager(IConfigurationDirectory directory) {
        this.directory = directory;
    }

    @Override
    public void addObject(Configurable<?> config) {
        loadedConfigurables.put(config, false);
    }

    public void loadConfigs() throws IOException {
        for (Iterator<Configurable<?>> iterator = loadedConfigurables.keySet().iterator(); iterator.hasNext();) {
            Configurable<?> configurable = iterator.next();
            loadConfig(configurable);
        }
        validateAllOrFail();
    }

    /**
     * Validates all loaded configurables against their JSON schemas and fails fast
     * if any schema violations are found. Validation is best-effort: a missing
     * schema is not an error (many configs predate the schema system).
     *
     * @throws IllegalStateException if any configurable fails schema validation
     */
    private void validateAllOrFail() {
        ConfigurationValidator validator = new ConfigurationValidator();
        ConfigurationValidator.ValidationStats stats = validator.validateAll(this);
        if (!stats.allPassed()) {
            String msg = "Configuration validation failed for " + stats.failureCount
                    + " component(s): " + stats.failedComponents
                    + " — fix the reported errors before processing begins.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        if (stats.getTotalValidated() > 0) {
            log.info("All {} validated configuration(s) passed schema checks.",
                    stats.getTotalValidated());
        }
    }

    public void loadConfig(Configurable<?> configurable) throws IOException {
        if (loadedConfigurables.get(configurable) == false) {
            List<Path> resources = directory.lookUpResource(configurable);
            configurable.processConfigs(resources);
            loadedConfigurables.put(configurable, true);
        }
    }

    public void loadConfigs(boolean forceReload) throws IOException {
        for (Iterator<Configurable<?>> iterator = loadedConfigurables.keySet().iterator(); iterator.hasNext();) {
            Configurable<?> configurable = iterator.next();

            List<Path> resources = directory.lookUpResource(configurable);

            configurable.processConfigs(resources);
        }
    }

    public IConfigurationDirectory getConfigurationDirectory() {
        return directory;
    }

    @Override
    public Set<Configurable<?>> findObjects(Class<? extends Configurable<?>> clazz) {
        Set<Configurable<?>> result = new HashSet<>();

        for (Iterator<Configurable<?>> iterator = loadedConfigurables.keySet().iterator(); iterator.hasNext();) {
            Configurable<?> configurable = iterator.next();
            if (configurable.getClass().equals(clazz)) {
                result.add(configurable);
            }
        }

        return result;
    }

    public <T extends Configurable<?>> T findObject(Class<T> clazz) {
        for (Configurable<?> configurable : this.loadedConfigurables.keySet()) {
            if (configurable.getClass().equals(clazz)) {
                return (T) configurable;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // ITypedConfigAccess implementation
    // -------------------------------------------------------------------------

    @Override
    public <T extends Configurable<?>> java.util.Optional<T> getConfig(Class<T> clazz) {
        return java.util.Optional.ofNullable(findObject(clazz));
    }

    @Override
    public boolean isTaskEnabled(String propertyName) {
        return getEnableTaskProperty(propertyName);
    }

    // -------------------------------------------------------------------------

    public AbstractTaskConfig<?> getTaskConfigurable(String configFileName) {
        for (Configurable<?> config : this.loadedConfigurables.keySet()) {
            if (config instanceof AbstractTaskConfig) {
                AbstractTaskConfig<?> taskConfig = (AbstractTaskConfig<?>) config;
                if (taskConfig.getTaskConfigFileName().equals(configFileName)) {
                    return taskConfig;
                }
            }
        }
        return null;
    }

    public EnableTaskProperty getEnableTaskConfigurable(String propertyName) {
        Set<Configurable<?>> configs = findObjects(EnableTaskProperty.class);
        for(Configurable<?> config : configs) {
            EnableTaskProperty enableProp = (EnableTaskProperty) config;
            if (enableProp.getPropertyName().equals(propertyName)) {
                return enableProp;
            }
        }
        return null;
    }

    public boolean getEnableTaskProperty(String propertyName) {
        EnableTaskProperty enableProp = this.getEnableTaskConfigurable(propertyName);
        if (enableProp != null) {
            return enableProp.isEnabled();
        } else {
            return false;
        }
    }

    @Override
    public Set<Configurable<?>> findObjects(String className) {
        Set<Configurable<?>> result = new HashSet<>();

        for (Iterator<Configurable<?>> iterator = loadedConfigurables.keySet().iterator(); iterator.hasNext();) {
            Configurable<?> configurable = iterator.next();
            if (configurable.getClass().getName().equals(className)) {
                result.add(configurable);
            }
        }

        return result;
    }

    @Override
    public Set<Configurable<?>> getObjects() {
        return loadedConfigurables.keySet();
    }

    @Override
    public void removeObject(Configurable<?> aObject) {
        loadedConfigurables.remove(aObject);
    }

    public void saveSerializedConfig(File file) throws FileNotFoundException, IOException {
        try(FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos)){
            oos.writeObject(loadedConfigurables);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadSerializedConfig(File file)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
            loadedConfigurables = (Map<Configurable<?>, Boolean>) ois.readObject();
        }
    }

}
