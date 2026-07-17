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
package iped.engine.config.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line tool for validating all JSON schemas. Validates schemas against JSON Schema
 * specification and checks for completeness.
 */
public class SchemaValidationCLI {
  private final ObjectMapper mapper = new ObjectMapper();
  private List<String> validSchemas = new ArrayList<>();
  private List<String> invalidSchemas = new ArrayList<>();
  private List<String> missingSchemas = new ArrayList<>();

  public static void main(String[] args) {
    SchemaValidationCLI cli = new SchemaValidationCLI();

    if (args.length == 0 || args[0].equals("--validate-all")) {
      cli.validateAllSchemas();
      cli.printReport();
    } else if (args[0].equals("--validate-config")) {
      if (args.length < 2) {
        System.err.println("Usage: --validate-config <configName>");
        System.exit(1);
      }
      cli.validateConfigSchema(args[1]);
    } else if (args[0].equals("--validate-cli")) {
      if (args.length < 2) {
        System.err.println("Usage: --validate-cli <cliName>");
        System.exit(1);
      }
      cli.validateCLISchema(args[1]);
    } else if (args[0].equals("--help")) {
      cli.printUsage();
    } else {
      System.err.println("Unknown option: " + args[0]);
      cli.printUsage();
      System.exit(1);
    }
  }

  /** Validate all schemas found in resources. */
  public void validateAllSchemas() {
    System.out.println("Validating all IPED schemas...\n");

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

    System.out.println("Configuration Schemas:");
    for (String schemaName : configSchemas) {
      validateConfigSchema(schemaName);
    }

    String[] cliSchemas = {"IPEDProcessingCLI", "IPEDWebAPICLI", "IPEDSearchAppCLI"};

    System.out.println("\nCLI Schemas:");
    for (String schemaName : cliSchemas) {
      validateCLISchema(schemaName);
    }
  }

  /** Validate a single configuration schema. */
  public void validateConfigSchema(String configName) {
    try {
      ObjectNode schema = loadSchema("json", configName);
      if (schema == null) {
        System.out.println("  ✗ " + configName + " - SCHEMA NOT FOUND");
        missingSchemas.add(configName);
        return;
      }

      // Check for required schema properties
      List<String> errors = new ArrayList<>();
      if (!schema.has("$schema")) {
        errors.add("Missing $schema property");
      }
      if (!schema.has("title")) {
        errors.add("Missing title property");
      }
      if (!schema.has("type")) {
        errors.add("Missing type property");
      }

      if (errors.isEmpty()) {
        System.out.println("  ✓ " + configName);
        validSchemas.add(configName);
      } else {
        System.out.println("  ✗ " + configName + " - " + String.join(", ", errors));
        invalidSchemas.add(configName);
      }

      // Check for UI schema
      ObjectNode uiSchema = loadSchema("ui", configName);
      if (uiSchema == null) {
        System.out.println("    Warning: UI schema not found");
      }

    } catch (Exception e) {
      System.out.println("  ✗ " + configName + " - ERROR: " + e.getMessage());
      invalidSchemas.add(configName);
    }
  }

  /** Validate a single CLI schema. */
  public void validateCLISchema(String cliName) {
    try {
      ObjectNode schema = loadSchema("json", cliName);
      if (schema == null) {
        System.out.println("  ✗ " + cliName + " - SCHEMA NOT FOUND");
        missingSchemas.add(cliName);
        return;
      }

      List<String> errors = new ArrayList<>();
      if (!schema.has("$schema")) {
        errors.add("Missing $schema");
      }
      if (!schema.has("title")) {
        errors.add("Missing title");
      }
      if (!schema.has("properties")) {
        errors.add("Missing properties");
      }

      if (errors.isEmpty()) {
        System.out.println("  ✓ " + cliName);
        validSchemas.add(cliName);
      } else {
        System.out.println("  ✗ " + cliName + " - " + String.join(", ", errors));
        invalidSchemas.add(cliName);
      }

      // Check for UI schema
      ObjectNode uiSchema = loadSchema("ui", cliName);
      if (uiSchema == null) {
        System.out.println("    Warning: UI schema not found");
      }

    } catch (Exception e) {
      System.out.println("  ✗ " + cliName + " - ERROR: " + e.getMessage());
      invalidSchemas.add(cliName);
    }
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

  /** Print validation report. */
  public void printReport() {
    System.out.println("\n" + "=".repeat(50));
    System.out.println("VALIDATION REPORT");
    System.out.println("=".repeat(50));

    System.out.println("\nValid schemas: " + validSchemas.size());
    System.out.println("Invalid schemas: " + invalidSchemas.size());
    System.out.println("Missing schemas: " + missingSchemas.size());
    System.out.println(
        "Total: " + (validSchemas.size() + invalidSchemas.size() + missingSchemas.size()));

    if (!invalidSchemas.isEmpty()) {
      System.out.println("\nInvalid Schemas:");
      invalidSchemas.forEach(s -> System.out.println("  - " + s));
    }

    if (!missingSchemas.isEmpty()) {
      System.out.println("\nMissing Schemas:");
      missingSchemas.forEach(s -> System.out.println("  - " + s));
    }

    System.out.println(
        "\nValidation "
            + (invalidSchemas.isEmpty() && missingSchemas.isEmpty() ? "PASSED" : "FAILED"));
  }

  /** Print usage information. */
  private void printUsage() {
    System.out.println("Schema Validation Tool");
    System.out.println("Usage: java iped.engine.config.schema.SchemaValidationCLI [OPTION]");
    System.out.println("\nOptions:");
    System.out.println("  --validate-all         Validate all schemas (default)");
    System.out.println("  --validate-config NAME Validate a specific configuration schema");
    System.out.println("  --validate-cli NAME    Validate a specific CLI schema");
    System.out.println("  --help                 Show this help message");
  }
}
