# IPED Command-Line Interface Schemas Summary

## Overview

Comprehensive JSON Schema and UI Schema definitions have been created for all IPED command-line interfaces. These schemas enable:

- **Validation** of command-line arguments
- **Documentation** of available options
- **UI Generation** for configuration tools
- **IDE Support** for argument completion
- **API Documentation** for programmatic integration

## Files Created

### Schema Files

#### 1. IPEDProcessingCLI.schema.json
- **Purpose**: Validates IPED processing application arguments
- **Application**: `java -jar iped.jar`
- **Main Class**: `iped.app.processing.Main`
- **Implementation**: `iped.app.processing.CmdLineArgsImpl`
- **Key Arguments**: 
  - `-d` / `--data` (datasources) - Required
  - `-o` / `--output` (output directory)
  - `-profile` (processing profile)
  - `--nogui` (text mode flag)
  - 25+ additional options
- **Location**: `schemas/json/IPEDProcessingCLI.schema.json`

#### 2. IPEDProcessingCLI.uischema.json
- **Purpose**: UI form definition for processing arguments
- **Widget Types**: text, checkbox, select, updown, password, textarea, url
- **Features**:
  - Appropriate input controls for each argument type
  - Helpful descriptions and placeholders
  - Enum dropdowns for profiles and timezones
  - Password input for encrypted sources
- **Location**: `schemas/ui/IPEDProcessingCLI.uischema.json`

#### 3. IPEDWebAPICLI.schema.json
- **Purpose**: Validates Web API server arguments
- **Application**: `java -jar iped-webapi.jar`
- **Main Class**: `iped.engine.webapi.Main`
- **Arguments**:
  - `--host` (server host, default: 0.0.0.0)
  - `--port` (server port, default: 8080)
  - `--sources` (sources endpoint URL) - Required
- **Location**: `schemas/json/IPEDWebAPICLI.schema.json`

#### 4. IPEDWebAPICLI.uischema.json
- **Purpose**: UI form definition for Web API arguments
- **Features**:
  - Text input for host/port
  - URL validator for sources endpoint
  - Helpful descriptions
- **Location**: `schemas/ui/IPEDWebAPICLI.uischema.json`

#### 5. IPEDSearchAppCLI.schema.json
- **Purpose**: Validates Search application arguments
- **Application**: `java -jar iped-search-app.jar`
- **Main Class**: `iped.app.ui.AppMain`
- **Arguments**:
  - `caseFolder` (single case path)
  - `casesFolder` (multi-case path)
- **Location**: `schemas/json/IPEDSearchAppCLI.schema.json`

#### 6. IPEDSearchAppCLI.uischema.json
- **Purpose**: UI form definition for Search application arguments
- **Features**: Text inputs for case folder paths
- **Location**: `schemas/ui/IPEDSearchAppCLI.uischema.json`

### Registry and Documentation

#### CLI-REGISTRY.json
- **Purpose**: Master registry of all CLI applications
- **Content**:
  - Application metadata (name, description, jar name, main class)
  - Schema references (JSON and UI)
  - Key arguments for each application
  - Usage examples for each CLI
  - Processing profiles documentation
  - Environment variables reference
  - Common options across applications
  - Implementation notes and tips
- **Location**: `schemas/CLI-REGISTRY.json`

#### CLI-REFERENCE.md
- **Purpose**: Comprehensive human-readable CLI reference
- **Sections**:
  - IPED Processing Application (comprehensive guide)
  - IPED Web API Server (deployment guide)
  - IPED Search Application (usage guide)
  - Common usage patterns with examples
  - Environment variables for memory and Java options
  - Performance tuning recommendations
  - Troubleshooting guide
- **Location**: `schemas/CLI-REFERENCE.md`

## Key Features

### JSON Schemas
- ✅ **JSON Schema Draft 2020-12** compliant
- ✅ **Type validation**: string, integer, boolean, array, object
- ✅ **Enum validation**: Processing profiles, timezones
- ✅ **Pattern matching**: Regular expressions for input validation
- ✅ **Required fields**: Enforced for mandatory arguments
- ✅ **Default values**: Specified for optional arguments
- ✅ **Examples**: Sample command-line invocations
- ✅ **Descriptions**: Complete documentation for each argument

### UI Schemas
- ✅ **React JsonSchema Form compatible**
- ✅ **Appropriate widgets**: text, select, checkbox, updown, password, textarea, url
- ✅ **Help text**: User-friendly descriptions
- ✅ **Placeholders**: Example values and formats
- ✅ **Custom options**: Array management, enum selections
- ✅ **Validation feedback**: Immediate error messages

## Processing Arguments by Category

### Input/Output
- `-d`, `--data` - Data sources to process
- `-o`, `--output` - Output case folder
- `-dname` - Display names for sources

### Processing Configuration
- `-profile` - Processing profile selection
- `-b`, `--blocksize` - Sector block size
- `-tz`, `--timezone` - FAT device timezone
- `-p`, `--password` - Encryption passwords

### Content Control
- `-l`, `--keywordlist` - Keywords for searching
- `-ocr` - Categories to OCR
- `-nocontent` - Categories to exclude from report

### Operation Modes
- `--append` - Add to existing case
- `--continue` - Resume processing
- `--restart` - Restart from beginning
- `--nogui` - Text mode processing

### Advanced Features
- `-X` - Module-specific options
- `--downloadInternetData` - Fetch internet data
- `--portable` - Portable case format
- `--addowner` - Index file owners
- `-splash` - Custom splash message

## Usage Examples

### Example 1: Forensic Investigation
```bash
java -jar iped.jar \
  -d /forensic/image.E01 \
  -dname "Suspect Laptop" \
  -o /cases/investigation_001 \
  -profile forensic \
  -l /keywords/suspects.txt \
  -tz GMT-5 \
  --nogui
```

### Example 2: CSAM Detection
```bash
java -jar iped.jar \
  -d /evidence/phone.ufdr \
  -dname "iPhone 13" \
  -o /cases/csam_investigation \
  -profile pedo \
  -ocr Images
```

### Example 3: Multi-Source Case
```bash
java -jar iped.jar \
  -d /phone -dname "Phone" \
  -d /laptop -dname "Laptop" \
  -d /network -dname "Server" \
  -o /cases/complex_case \
  -profile forensic \
  --append
```

### Example 4: Web API Deployment
```bash
java -Xmx16g -jar iped-webapi.jar \
  --host=192.168.1.100 \
  --port=8080 \
  --sources=http://evidence-server/api/sources
```

## Integration Options

### 1. Argument Validation
```java
SchemaValidator validator = new SchemaValidator();
ObjectMapper mapper = new ObjectMapper();

JsonNode schema = mapper.readTree(
  new File("schemas/json/IPEDProcessingCLI.schema.json")
);

String configJson = mapper.writeValueAsString(cmdLineArgs);
validator.validateJson(configJson, (ObjectNode) schema);
```

### 2. IDE Integration
IDEs can use these schemas for:
- **Argument completion** in terminal/run configurations
- **Documentation hover** showing argument descriptions
- **Quick fixes** for invalid arguments
- **Parameter hints** while typing

### 3. Web UI Form Generation
```javascript
const schema = require('./IPEDProcessingCLI.schema.json');
const uiSchema = require('./IPEDProcessingCLI.uischema.json');

<Form schema={schema} uiSchema={uiSchema} onChange={handleChange} />
```

### 4. API Documentation
OpenAPI/Swagger can be generated from these schemas for API documentation and client generation.

### 5. Shell Completion Scripts
Bash/Zsh completion scripts can be generated to provide argument suggestions.

## Validation Coverage

### Input Validation
- ✅ Required arguments enforced
- ✅ File/folder existence validation
- ✅ Enum value validation (profiles, timezones)
- ✅ Type checking (integer, boolean, string, array)
- ✅ Pattern matching for custom formats

### Consistency Checks
- ✅ One-of validation (datasources OR evidence removal)
- ✅ Dependent argument relationships
- ✅ Multiple argument coordination

## Performance Tips

### Memory Configuration
```bash
java -Xmx16g -jar iped.jar ...   # For large cases
java -Xmx32g -jar iped-webapi.jar ... # For server deployment
```

### Processing Optimization
```bash
java -jar iped.jar -d /source --nogui  # Headless processing
java -jar iped.jar -d /local/folder -o /output  # No network overhead
```

### Timezone Handling
```bash
# Explicitly specify timezone instead of using system default
java -jar iped.jar -d /source -o /output -tz GMT-3
```

## Future Enhancements

- [ ] Shell completion script generation
- [ ] IDE plugin for argument suggestions
- [ ] Web UI builder from schema
- [ ] Argument history tracking
- [ ] Dry-run validation mode
- [ ] Configuration templates for common scenarios

## Statistics

| Category | Count |
|----------|-------|
| CLI Applications | 3 |
| JSON Schemas | 3 |
| UI Schemas | 3 |
| Registry Files | 1 |
| Documentation Files | 2 |
| Total Arguments Documented | 30+ |

## Locations

All CLI schemas and documentation are located in:
```
iped-engine/src/main/resources/schemas/
├── json/
│   ├── IPEDProcessingCLI.schema.json
│   ├── IPEDWebAPICLI.schema.json
│   └── IPEDSearchAppCLI.schema.json
├── ui/
│   ├── IPEDProcessingCLI.uischema.json
│   ├── IPEDWebAPICLI.uischema.json
│   └── IPEDSearchAppCLI.uischema.json
├── CLI-REGISTRY.json
└── CLI-REFERENCE.md
```

## References

- [JSON Schema Specification](https://json-schema.org/)
- [React JsonSchema Form](https://rjsf-team.github.io/react-jsonschema-form/)
- IPED Command-Line Implementation: `iped.app.processing.CmdLineArgsImpl`
- IPED CLI Interface: `iped.engine.CmdLineArgs`
