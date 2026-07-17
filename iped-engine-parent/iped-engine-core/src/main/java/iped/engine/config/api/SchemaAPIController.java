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
package iped.engine.config.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import iped.engine.config.schema.SchemaValidator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * API controller for schema operations. Provides REST endpoints for accessing configuration and CLI
 * schemas.
 */
@Slf4j
public class SchemaAPIController {
  private final ObjectMapper mapper = new ObjectMapper();
  private final SchemaValidator validator = new SchemaValidator();

  /**
   * Get list of all available schemas.
   *
   * @return List of schema metadata
   */
  public List<Map<String, Object>> listSchemas() {
    List<Map<String, Object>> schemas = new ArrayList<>();

    String[] configSchemas = {
      "TaskInstallerConfig",
      "ParsersConfig",
      "ExternalParsersConfig",
      "AnalysisConfig",
      "OCRConfig",
      "FileSystemConfig",
      "LocalConfig",
      "PluginConfig",
      "ProcessingPriorityConfig",
      "IndexTaskConfig",
      "ElasticSearchTaskConfig",
      "HashTaskConfig",
      "ImageThumbTaskConfig",
      "VideoThumbsConfig",
      "AudioTranscriptConfig",
      "RegexTaskConfig",
      "NamedEntityTaskConfig",
      "CategoryConfig",
      "SignatureConfig",
      "MakePreviewConfig",
      "RemoteImageClassifierConfig",
      "AIFiltersConfig",
      "PhotoDNAConfig",
      "PhotoDNALookupConfig",
      "ExportByCategoriesConfig",
      "ParsingTaskConfig",
      "TempFileTaskConfig",
      "DocThumbTaskConfig",
      "LocaleConfig",
      "AbstractTaskConfig",
      "AbstractTaskPropertiesConfig",
      "AgeEstimationConfig",
      "FaceRecognitionConfig",
      "SplitLargeBinaryConfig",
      "ProcessingOrchestratorConfig",
      "SplashScreenConfig",
      "CategoryToExpandConfig",
      "DefaultTaskPropertiesConfig",
      "ExportByKeywordsConfig",
      "HashDBLookupConfig",
      "HtmlReportTaskConfig",
      "AppIDsConfig",
      "CarverTaskConfig",
      "GraphTaskConfig",
      "MapPanelConfig",
      "AbstractPropertiesConfigurable"
    };

    for (String schemaName : configSchemas) {
      schemas.add(createSchemaMetadata(schemaName, "configuration"));
    }

    String[] cliSchemas = {"IPEDProcessingCLI", "IPEDWebAPICLI", "IPEDSearchAppCLI"};

    for (String schemaName : cliSchemas) {
      schemas.add(createSchemaMetadata(schemaName, "cli"));
    }

    return schemas;
  }

  /**
   * Get a specific configuration schema.
   *
   * @param componentName Name of the configuration component
   * @return The schema JSON
   */
  public Map<String, Object> getSchema(String componentName) {
    try {
      ObjectNode schema = loadSchema("json", componentName);
      if (schema == null) {
        return errorResponse("Schema not found: " + componentName);
      }

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("schema", mapper.convertValue(schema, Object.class));

      ObjectNode uiSchema = loadSchema("ui", componentName);
      if (uiSchema != null) {
        response.put("uiSchema", mapper.convertValue(uiSchema, Object.class));
      }

      return response;
    } catch (Exception e) {
      log.error("Error loading schema: " + componentName, e);
      return errorResponse("Error loading schema: " + e.getMessage());
    }
  }

  /**
   * Get a CLI schema.
   *
   * @param appName Name of the CLI application
   * @return The CLI schema JSON
   */
  public Map<String, Object> getCLISchema(String appName) {
    return getSchema(appName);
  }

  /**
   * Validate a configuration against its schema.
   *
   * @param componentName Configuration component name
   * @param configJson Configuration JSON to validate
   * @return Validation result
   */
  public Map<String, Object> validateConfiguration(String componentName, String configJson) {
    try {
      ObjectNode schema = loadSchema("json", componentName);
      if (schema == null) {
        return errorResponse("Schema not found for: " + componentName);
      }

      SchemaValidator.ValidationResult result = validator.validateJson(configJson, schema);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("valid", result.isValid());
      response.put("errors", result.getErrors());

      if (!result.isValid()) {
        response.put("errorReport", result.getErrorReport());
      }

      return response;
    } catch (Exception e) {
      log.error("Error validating configuration", e);
      return errorResponse("Validation error: " + e.getMessage());
    }
  }

  /**
   * Get all available CLI schemas.
   *
   * @return List of CLI schemas
   */
  public List<Map<String, Object>> listCLISchemas() {
    List<Map<String, Object>> cliSchemas = new ArrayList<>();

    String[] apps = {"IPEDProcessingCLI", "IPEDWebAPICLI", "IPEDSearchAppCLI"};

    for (String appName : apps) {
      cliSchemas.add(createSchemaMetadata(appName, "cli"));
    }

    return cliSchemas;
  }

  /**
   * Get schema by category.
   *
   * @param category Schema category
   * @return List of schemas in category
   */
  public List<Map<String, Object>> getSchemasByCategory(String category) {
    List<Map<String, Object>> schemas = new ArrayList<>();
    List<Map<String, Object>> allSchemas = listSchemas();

    for (Map<String, Object> schema : allSchemas) {
      if (category.equals(schema.get("category"))) {
        schemas.add(schema);
      }
    }

    return schemas;
  }

  /** Load a schema from resources. */
  private ObjectNode loadSchema(String type, String name) throws IOException {
    String path = "/schemas/" + type + "/" + name + ".schema.json";
    InputStream stream = getClass().getResourceAsStream(path);

    if (stream == null) {
      return null;
    }

    try {
      return (ObjectNode) mapper.readTree(stream);
    } finally {
      stream.close();
    }
  }

  /** Create schema metadata. */
  private Map<String, Object> createSchemaMetadata(String schemaName, String type) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("name", schemaName);
    metadata.put("type", type);
    metadata.put("jsonSchemaUrl", "/api/schemas/" + schemaName);
    metadata.put("uiSchemaUrl", "/api/schemas/" + schemaName + "/ui");
    metadata.put("validationUrl", "/api/schemas/" + schemaName + "/validate");

    // Determine category
    String category = determineCategory(schemaName);
    metadata.put("category", category);

    return metadata;
  }

  /** Determine schema category based on name. */
  private String determineCategory(String schemaName) {
    if (schemaName.contains("CLI")) {
      return "cli";
    } else if (schemaName.contains("Task") || schemaName.contains("Config")) {
      return "configuration";
    } else {
      return "general";
    }
  }

  /** Create error response. */
  private Map<String, Object> errorResponse(String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("success", false);
    response.put("error", message);
    return response;
  }
}
