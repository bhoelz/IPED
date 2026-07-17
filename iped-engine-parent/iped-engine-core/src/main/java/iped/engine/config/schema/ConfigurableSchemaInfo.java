package iped.engine.config.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata about a Configurable component's schema. Contains all information needed to generate
 * JSON Schema and UI Schema.
 */
public class ConfigurableSchemaInfo {
  private String componentName;
  private String description;
  private String configurableClassName;
  private String configurationTypeGeneric;
  private String resourceLookupPattern;
  private String configFileName;
  private List<ConfigurableProperty> properties = new ArrayList<>();
  private String version = "1.0";

  public ConfigurableSchemaInfo(String componentName, String configurableClassName) {
    this.componentName = componentName;
    this.configurableClassName = configurableClassName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getConfigurableClassName() {
    return configurableClassName;
  }

  public void setConfigurableClassName(String configurableClassName) {
    this.configurableClassName = configurableClassName;
  }

  public String getConfigurationTypeGeneric() {
    return configurationTypeGeneric;
  }

  public void setConfigurationTypeGeneric(String configurationTypeGeneric) {
    this.configurationTypeGeneric = configurationTypeGeneric;
  }

  public String getResourceLookupPattern() {
    return resourceLookupPattern;
  }

  public void setResourceLookupPattern(String resourceLookupPattern) {
    this.resourceLookupPattern = resourceLookupPattern;
  }

  public String getConfigFileName() {
    return configFileName;
  }

  public void setConfigFileName(String configFileName) {
    this.configFileName = configFileName;
  }

  public List<ConfigurableProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<ConfigurableProperty> properties) {
    this.properties = properties;
  }

  public void addProperty(ConfigurableProperty property) {
    this.properties.add(property);
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
