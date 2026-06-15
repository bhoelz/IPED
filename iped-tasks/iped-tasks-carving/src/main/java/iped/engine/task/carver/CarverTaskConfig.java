package iped.engine.task.carver;

import iped.engine.config.AbstractTaskConfig;
import iped.engine.config.Configuration;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

public class CarverTaskConfig extends AbstractTaskConfig<XMLCarverConfiguration> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String ENABLE_PARAM = "enableCarving"; //$NON-NLS-1$
    public static final String GLOBAL_CARVER_CONFIG = "CarverConfig.xml"; //$NON-NLS-1$
    public static final String GLOBAL_CARVER_CONFIG_TOML = "CarverConfig.toml"; //$NON-NLS-1$
    public static final String CARVER_CONFIG_PREFIX = "carver-"; //$NON-NLS-1$
    public static final String CARVER_CONFIG_SUFFIX = ".xml"; //$NON-NLS-1$

    private XMLCarverConfiguration carverConfiguration = new XMLCarverConfiguration();

    @Override
    public Filter<Path> getResourceLookupFilter() {
        return new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return entry.endsWith(Configuration.CONFIG_FILE)
                        || entry.endsWith(getTaskConfigFileName())
                        || entry.endsWith(GLOBAL_CARVER_CONFIG_TOML)
                        || (entry.getFileName() != null
                                && entry.getFileName().toString().startsWith(CARVER_CONFIG_PREFIX)
                                && entry.getFileName().toString().endsWith(CARVER_CONFIG_SUFFIX));
            }
        };
    }

    @Override
    public XMLCarverConfiguration getConfiguration() {
        return carverConfiguration;
    }

    @Override
    public void setConfiguration(XMLCarverConfiguration config) {
        this.carverConfiguration = config;
    }

    @Override
    public String getTaskEnableProperty() {
        return ENABLE_PARAM;
    }

    @Override
    public String getTaskConfigFileName() {
        return GLOBAL_CARVER_CONFIG;
    }

    @Override
    public void processTaskConfig(Path resource) throws IOException {

        if (resource.endsWith(GLOBAL_CARVER_CONFIG_TOML)) {
            TomlCarverConfiguration toml = new TomlCarverConfiguration();
            toml.loadTomlConfigFile(resource.toFile());
            carverConfiguration = toml;
            return;
        }

        // XML files: applied on top of whatever configuration is already loaded,
        // allowing user-supplied XML overrides to augment or replace TOML defaults.
        if (resource.endsWith(GLOBAL_CARVER_CONFIG)) {
            carverConfiguration.loadXMLConfigFile(resource.toFile());
        }
        String fileName = resource.getFileName() != null ? resource.getFileName().toString() : "";
        if (fileName.startsWith(CARVER_CONFIG_PREFIX) && fileName.endsWith(CARVER_CONFIG_SUFFIX)) {
            carverConfiguration.loadXMLConfigFile(resource.toFile());
        }
    }
}
