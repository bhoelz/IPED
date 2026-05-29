# IPED Configuration Schema Registry

This document catalogs all available JSON Schemas and UI Schemas for configurable components in IPED.

## Overview

IPED configuration is managed through the `Configurable<T>` interface, which defines how components load and persist configuration. This registry provides:

- **JSON Schemas** for validation and documentation
- **UI Schemas** for form generation
- **Metadata** about configuration files and properties
- **Examples** of valid configurations

All schemas conform to **JSON Schema Draft 2020-12** for maximum compatibility with tools and libraries.

## Core Configuration Components

### Task Installer Configuration
- **File**: `TaskInstallerConfig.schema.json`
- **Class**: `iped.engine.config.TaskInstallerConfig`
- **Resource**: `TaskInstaller.xml`
- **Description**: Defines the sequence of processing tasks and how tasks are installed/uninstalled
- **Configuration Type**: String (XML)
- **Key Properties**:
  - `tasks`: Array of task definitions (class or script-based)
  - Task order is critical - dependencies must be respected

### Parsers Configuration
- **File**: `ParsersConfig.schema.json`
- **Class**: `iped.engine.config.ParsersConfig`
- **Resource**: `ParserConfig.xml`
- **Description**: Configures Tika parsers for content extraction
- **Configuration Type**: String (XML)
- **Key Properties**:
  - `parsers`: Array of parser configurations
  - `params`: Parser-specific parameters
  - `mimeExclusions`: MIME types to skip

### External Parsers Configuration
- **File**: `ExternalParsersConfig.schema.json`
- **Class**: `iped.engine.config.ExternalParsersConfig`
- **Resource**: `ExternalParsers.xml`
- **Description**: Integrates external command-line tools as parsers
- **Configuration Type**: String (XML)
- **Key Properties**:
  - `parsers`: Array of external tool definitions
  - `command`: Command line with `${INPUT}` placeholder
  - `mimeTypes`: File types this tool handles
  - `metadata`: Regex patterns to extract metadata from tool output

### Map Panel Configuration
- **File**: `MapPanelConfig.schema.json`
- **Class**: `iped.geo.impl.MapPanelConfig`
- **Resource**: `GEOConfig.txt`
- **Description**: Configures geographic mapping and map tile servers
- **Configuration Type**: UTF8Properties
- **Key Properties**:
  - `tileServerUrlPattern`: Default tile server URL with placeholders
  - `tileServers`: Named tile server options (Bing, OpenStreetMap, Esri, etc.)

## Analysis & Processing Configuration

### Analysis Configuration
- **File**: `AnalysisConfig.schema.json`
- **Class**: `iped.engine.config.AnalysisConfig`
- **Resource**: `AnalysisConfig.txt`
- **Description**: General application-wide analysis and processing settings
- **Configuration Type**: UTF8Properties
- **Key Properties**:
  - `embedLibreOffice`: Include LibreOffice for document conversion
  - `searchThreads`: Parallelism for searching
  - `maxBackups`: Number of case backups to retain
  - `openWithDoubleClick`: How to handle executable files

### OCR Configuration
- **File**: `OCRConfig.schema.json`
- **Class**: `iped.engine.config.OCRConfig`
- **Resource**: `OCRConfig.txt`
- **Description**: Optical character recognition settings for image text extraction
- **Configuration Type**: UTF8Properties
- **Key Properties**:
  - `enableOCR`: Enable/disable OCR processing
  - `ocrLanguage`: Language code (e.g., "eng", "por")
  - `pdfToImgResolution`: DPI for PDF to image conversion
  - `pageSegMode`: Tesseract page segmentation algorithm

## Task Configurations

### Index Task Configuration
- **File**: `IndexTaskConfig.schema.json`
- **Class**: `iped.engine.config.IndexTaskConfig`
- **Resource**: `IndexTaskConfig.txt`
- **Description**: Elasticsearch indexing and search optimization
- **Configuration Type**: UTF8Properties
- **Key Properties**:
  - `indexUnallocated`: Include unallocated space
  - `convertCharsToLowerCase`: Case-insensitive search
  - `convertCharsToAscii`: Diacritical normalization
  - `textSplitSize`: Maximum text chunk size
  - `forceMerge`: Single-segment optimization

### Hash Task Configuration
- **File**: `HashTaskConfig.schema.json`
- **Class**: `iped.engine.config.HashTaskConfig`
- **Resource**: `HashTaskConfig.txt`
- **Description**: File hashing and known hash database lookup
- **Configuration Type**: UTF8Properties
- **Key Properties**:
  - `hashAlgorithm`: MD5, SHA-1, or SHA-256
  - `enableHashDatabase`: Enable/disable database lookups
  - `hashDatabasePath`: Path to hash database files

## Abstract Base Classes

### AbstractPropertiesConfigurable
- **File**: `AbstractPropertiesConfigurable.schema.json`
- **Class**: `iped.engine.config.AbstractPropertiesConfigurable`
- **Description**: Base class for properties-file-based configuration
- **Configuration Type**: UTF8Properties
- **Subclasses**: 10+ implementations
  - AnalysisConfig
  - OCRConfig
  - FileSystemConfig
  - LocalConfig
  - PluginConfig
  - ProcessingPriorityConfig
  - And more...

## Schema File Organization

```
schemas/
├── json/
│   ├── TaskInstallerConfig.schema.json
│   ├── ParsersConfig.schema.json
│   ├── ExternalParsersConfig.schema.json
│   ├── MapPanelConfig.schema.json
│   ├── AnalysisConfig.schema.json
│   ├── OCRConfig.schema.json
│   ├── IndexTaskConfig.schema.json
│   ├── HashTaskConfig.schema.json
│   ├── AbstractPropertiesConfigurable.schema.json
│   └── [35+ more schemas]
├── ui/
│   ├── TaskInstallerConfig.uischema.json
│   ├── ParsersConfig.uischema.json
│   └── [corresponding UI schemas]
├── schema-index.json (this registry)
└── SCHEMA_REGISTRY.md (this documentation)
```

## Using Schemas

### Validation
Use `SchemaValidator` class to validate configurations:

```java
ConfigurableSchemaGenerator generator = new ConfigurableSchemaGenerator();
SchemaValidator validator = new SchemaValidator();

ConfigurableSchemaInfo info = generator.analyzeConfigurable(MyConfig.class);
ObjectNode schema = generator.generateJsonSchema(info);

ValidationResult result = validator.validateJson(jsonString, schema);
if (!result.isValid()) {
    System.out.println(result.getErrorReport());
}
```

### Form Generation
Use UI Schemas with form renderers:

```javascript
// react-jsonschema-form example
import Form from "@rjsf/core";

const jsonSchema = require('./ParsersConfig.schema.json');
const uiSchema = require('./ParsersConfig.uischema.json');

<Form schema={jsonSchema} uiSchema={uiSchema} onSubmit={handleSubmit} />
```

### Documentation
Each schema includes:
- `title`: Human-readable component name
- `description`: What the component configures
- `properties`: Detailed property descriptions
- `examples`: Sample valid configurations

## Schema Standards

All schemas follow these conventions:

1. **JSON Schema Draft 2020-12**: Maximum tool compatibility
2. **$id**: Unique identifier with schema repository URL
3. **$schema**: Explicit schema version declaration
4. **additionalProperties: false**: Strict property validation
5. **required**: Marks mandatory properties
6. **examples**: Includes valid configuration examples

UI Schemas follow these conventions:

1. **React JsonSchema Form (RJSF) compatible**
2. **ui:widget**: Specifies input control type
3. **ui:help**: Provides user-facing descriptions
4. **ui:options**: Advanced control configuration
5. **ui:title**: Override property display name

## Extending Schemas

To add schemas for new Configurable implementations:

1. Create JSON schema file in `schemas/json/` named `{ComponentName}.schema.json`
2. Create UI schema file in `schemas/ui/` named `{ComponentName}.uischema.json`
3. Add entry to `schema-index.json` with metadata
4. Update this registry documentation

## Planned Enhancements

- [ ] Complete schemas for all 45+ Configurable implementations
- [ ] Add runtime validation on configuration load
- [ ] Web-based configuration UI generator
- [ ] Configuration migration scripts
- [ ] Schema versioning and evolution support
- [ ] Multi-language documentation

## References

- JSON Schema: https://json-schema.org/
- React JsonSchema Form: https://rjsf-team.github.io/react-jsonschema-form/
- IPED Configuration: See `iped-engine/src/main/java/iped/engine/config/`
- Configurable Interface: `iped-api/src/main/java/iped/configuration/Configurable.java`
