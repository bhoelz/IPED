package iped.engine.config;

import iped.configuration.Configurable;
import iped.utils.TomlProperties;
import iped.utils.UTF8Properties;
import java.io.IOException;
import java.nio.file.Path;

public abstract class AbstractPropertiesConfigurable implements Configurable<UTF8Properties> {

  /** */
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
  public void processConfig(Path resource) throws IOException {
    // TOML files are flattened to properties; loading layers into the same
    // instance gives key-level last-wins merge over built-in defaults.
    // Files.newInputStream based loading also works inside plugin/defaults JARs.
    TomlProperties toml = new TomlProperties();
    toml.load(resource);
    properties.putAll(toml);
    processProperties(properties);
  }

  protected abstract void processProperties(UTF8Properties properties);
}
