package iped.engine.config;

import iped.utils.TomlProperties;
import iped.utils.UTF8Properties;

import java.io.IOException;
import java.nio.file.Path;

public abstract class AbstractTaskPropertiesConfig extends AbstractTaskConfig<UTF8Properties> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    protected UTF8Properties properties = new UTF8Properties();

    @Override
    public UTF8Properties getConfiguration() {
        return properties;
    }

    @Override
    public void setConfiguration(UTF8Properties config) {
        this.properties = config;
    }

    @Override
    public void processTaskConfig(Path resource) throws IOException {
        // TOML layers loaded into the same instance merge key-level last-wins,
        // so local files only need to contain deviations from built-in defaults
        TomlProperties toml = new TomlProperties();
        toml.load(resource);
        properties.putAll(toml);
        processProperties(properties);
    }

    abstract void processProperties(UTF8Properties properties);

}
