# IPED Schema Tools Guide

## Overview

Two command-line tools are provided for working with IPED configuration and CLI schemas:

1. **CLI Help Generator** - Auto-generate help text from schemas
2. **Schema Validation Tool** - Validate all schemas for correctness

## CLI Help Generator

### Purpose
Automatically generate formatted help text from JSON schemas, eliminating manual documentation maintenance.

### Usage

```bash
# Generate help for a specific CLI application
java -cp iped.jar iped.engine.config.schema.CLIHelpGenerator IPEDProcessingCLI

# Generate help for all CLI applications
java -cp iped.jar iped.engine.config.schema.CLIHelpGenerator --all
```

### Features

- **Title and Description**: Extracts title and description from schema
- **Argument Categorization**: Separates required vs optional arguments
- **Type Information**: Shows argument types (string, integer, boolean, etc.)
- **Descriptions**: Includes detailed descriptions from schema
- **Default Values**: Displays default values when specified
- **Enum Values**: Lists valid options for enumerated fields
- **Formatted Output**: Clean, readable help text with proper indentation

### Example Output

```
IPED Processing Application

Configuration and processing parameters for digital forensic investigation

USAGE:
  Required arguments:
    -d, --data          [string]       Data source paths
                                       Path to forensic image or evidence folder
    -o, --output        [string]       Output case folder
                                       Directory where case results will be stored

  Optional arguments:
    -profile            [string]       Processing profile
                                       Default: forensic
                                       Valid values: forensic, pedo, fastmode, blind, triage
    -tz, --timezone     [string]       FAT device timezone
                                       Default: GMT
    -nogui              [boolean]      Text mode processing
```

## Schema Validation Tool

### Purpose
Validate all configuration and CLI schemas against JSON Schema specification.

### Usage

```bash
# Validate all schemas
java -cp iped.jar iped.engine.config.schema.SchemaValidationCLI --validate-all

# Validate a specific configuration schema
java -cp iped.jar iped.engine.config.schema.SchemaValidationCLI --validate-config AnalysisConfig

# Validate a specific CLI schema
java -cp iped.jar iped.engine.config.schema.SchemaValidationCLI --validate-cli IPEDProcessingCLI

# Display help
java -cp iped.jar iped.engine.config.schema.SchemaValidationCLI --help
```

### Validation Checks

The tool validates:

1. **JSON Schema Properties**
   - `$schema` - Schema version declaration
   - `title` - Human-readable title
   - `type` - Data type specification
   - `properties` - Property definitions (for CLIs)
   - `description` - Detailed description

2. **Schema Completeness**
   - Required properties are defined
   - Descriptions are present
   - Data types are valid

3. **UI Schema Correspondence**
   - UI schema exists for each JSON schema
   - UI schema format is valid

4. **Schema Accessibility**
   - Schemas are loadable from resources
   - No file I/O errors

### Example Output

```
Validating all IPED schemas...

Configuration Schemas:
  ✓ TaskInstallerConfig
  ✓ ParsersConfig
  ✓ ExternalParsersConfig
  ✓ AnalysisConfig
  ✓ OCRConfig
  ...
  ✗ UnknownConfig - Missing type property

CLI Schemas:
  ✓ IPEDProcessingCLI
  ✓ IPEDWebAPICLI
  ✓ IPEDSearchAppCLI

==================================================
VALIDATION REPORT
==================================================

Valid schemas: 47
Invalid schemas: 1
Missing schemas: 0
Total: 48

Invalid Schemas:
  - UnknownConfig

Validation FAILED
```

## Integration with Build Process

### Maven Build

Add to your `pom.xml` to validate schemas during build:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>iped.engine.config.schema.SchemaValidationCLI</mainClass>
                <arguments>
                    <argument>--validate-all</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### CI/CD Pipeline

```bash
#!/bin/bash
# In your CI pipeline

java -cp target/iped-engine-4.4.0-SNAPSHOT.jar \
    iped.engine.config.schema.SchemaValidationCLI --validate-all

if [ $? -ne 0 ]; then
    echo "Schema validation failed!"
    exit 1
fi
```

## Troubleshooting

### Schema Not Found
- Ensure schema files exist in `src/main/resources/schemas/json/` and `schemas/ui/`
- Check schema filename matches class name (e.g., `AnalysisConfig.schema.json`)

### Missing Properties in Schema
- Ensure schema has `$schema`, `title`, and `type` properties
- For CLI schemas, ensure `properties` object is defined

### Validation Failures
- Check JSON syntax with a JSON validator
- Verify schema follows JSON Schema Draft 2020-12 spec
- Ensure all referenced properties have type definitions

### UI Schema Issues
- Create corresponding `.uischema.json` file with same base name
- Define `ui:widget` and `ui:title` for user-friendly form controls
- See examples in `schemas/ui/` directory

## Best Practices

### For Help Text Generation

1. **Use Descriptive Titles**: Make schema title self-explanatory
2. **Write Clear Descriptions**: Include examples in descriptions
3. **Specify Defaults**: Always include `"default"` for optional fields with preset values
4. **List Enum Values**: Use `"enum"` array for fields with restricted choices
5. **Type Accuracy**: Use correct JSON types (string, integer, boolean, array, object)

### For Schema Validation

1. **Run Before Commits**: Validate schemas before code review
2. **Test Regularly**: Include schema validation in CI pipeline
3. **Document Changes**: Update schema-index.json when adding schemas
4. **Version Schemas**: Use `$id` with version information
5. **Validate Configs**: Test real configuration files against schemas

## Related Tools

- **ConfigurationValidator** - Runtime validation of loaded configurations
- **ConfigurableSchemaGenerator** - Generate schemas from component metadata
- **SchemaValidator** - Programmatic schema validation in Java code

## See Also

- [SCHEMA_REGISTRY.md](SCHEMA_REGISTRY.md) - Complete schema reference
- [CLI-REFERENCE.md](CLI-REFERENCE.md) - CLI argument documentation
- [NEXT-STEPS.md](NEXT-STEPS.md) - Implementation roadmap
- [JSON Schema Specification](https://json-schema.org/) - Schema standard reference
