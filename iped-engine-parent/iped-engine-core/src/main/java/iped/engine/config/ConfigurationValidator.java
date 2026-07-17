/*
 * Copyright 2012-2014, Luis Filipe da Cruz Nassif
 *
 * This file is part of Indexador e Processador de Evidências Digitais (IPED).
 *
 * IPED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IPED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IPED.  If not, see <http://www.gnu.org/licenses/>.
 */
package iped.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import iped.configuration.Configurable;
import iped.engine.config.schema.SchemaValidator;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates configurations against their JSON schemas. Provides runtime validation of all
 * Configurable components.
 */
@Slf4j
public class ConfigurationValidator {

  private final SchemaValidator schemaValidator = new SchemaValidator();
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, ObjectNode> schemaCache = new HashMap<>();

  private static final String SCHEMA_RESOURCE_PATH = "/schemas/json/";
  private static final String SCHEMA_FILE_SUFFIX = ".schema.json";

  /**
   * Validates a single configuration object against its schema. Logs validation results; does not
   * throw exceptions.
   *
   * @param config The configuration object to validate
   * @param configurableClass The class implementing Configurable
   * @return true if validation passed, false otherwise
   */
  public boolean validateConfiguration(Object config, Class<?> configurableClass) {
    try {
      String componentName = getComponentName(configurableClass);
      ObjectNode schema = loadSchema(componentName);

      if (schema == null) {
        log.debug("No schema found for component: {}", componentName);
        return true;
      }

      SchemaValidator.ValidationResult result = schemaValidator.validate(config, schema);

      if (!result.isValid()) {
        log.warn(
            "Configuration validation failed for {}:\n{}", componentName, result.getErrorReport());
        return false;
      }

      log.debug("Configuration validation passed for: {}", componentName);
      return true;

    } catch (Exception e) {
      log.error("Error during configuration validation", e);
      return false;
    }
  }

  /**
   * Validates all configurations in a ConfigurationManager. Returns statistics about validation
   * results.
   *
   * @param configManager The configuration manager to validate
   * @return ValidationStats with success/failure counts
   */
  public ValidationStats validateAll(ConfigurationManager configManager) {
    ValidationStats stats = new ValidationStats();

    try {
      for (Configurable<?> configurable : configManager.getObjects()) {
        Class<?> configurableClass = configurable.getClass();
        // Validate the canonical config snapshot, not the live engine object: the
        // latter can carry expensive derived getters (e.g. TaskInstallerConfig's
        // task-graph resolution) or third-party object graphs Jackson can't safely
        // walk, neither of which the JSON schemas describe.
        boolean valid = validateConfiguration(configurable.getConfiguration(), configurableClass);

        if (valid) {
          stats.successCount++;
        } else {
          stats.failureCount++;
          stats.failedComponents.add(configurableClass.getSimpleName());
        }
      }

      log.info(
          "Configuration validation summary: {} passed, {} failed",
          stats.successCount,
          stats.failureCount);

    } catch (Exception e) {
      log.error("Error validating configuration manager", e);
    }

    return stats;
  }

  /**
   * Loads a schema from resources. Caches schemas for performance.
   *
   * @param componentName The name of the component
   * @return The loaded schema, or null if not found
   * @throws IOException If the schema file is invalid JSON
   */
  private ObjectNode loadSchema(String componentName) throws IOException {
    if (schemaCache.containsKey(componentName)) {
      return schemaCache.get(componentName);
    }

    String schemaPath = SCHEMA_RESOURCE_PATH + componentName + SCHEMA_FILE_SUFFIX;
    InputStream resourceStream = getClass().getResourceAsStream(schemaPath);

    if (resourceStream == null) {
      return null;
    }

    try {
      ObjectNode schema = (ObjectNode) mapper.readTree(resourceStream);
      schemaCache.put(componentName, schema);
      return schema;
    } finally {
      resourceStream.close();
    }
  }

  /**
   * Derives component name from Configurable class name. Maps AnalysisConfig to AnalysisConfig,
   * etc.
   *
   * @param configurableClass The Configurable implementation class
   * @return The schema component name
   */
  private String getComponentName(Class<?> configurableClass) {
    String simpleName = configurableClass.getSimpleName();

    if (simpleName.endsWith("Config")) {
      return simpleName;
    }

    return simpleName + "Config";
  }

  /** Statistics about validation run. */
  public static class ValidationStats {
    public int successCount = 0;
    public int failureCount = 0;
    public java.util.List<String> failedComponents = new java.util.ArrayList<>();

    public int getTotalValidated() {
      return successCount + failureCount;
    }

    public boolean allPassed() {
      return failureCount == 0;
    }

    @Override
    public String toString() {
      return String.format(
          "ValidationStats{success=%d, failures=%d, total=%d}",
          successCount, failureCount, getTotalValidated());
    }
  }
}
