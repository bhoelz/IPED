# IPED Configuration API Reference

## Overview

The IPED Configuration API provides REST endpoints for managing and validating configurations through HTTP. This enables web UIs, external tools, and programmatic access to configuration management.

**Base URL**: `/api/v1`

**Content-Type**: `application/json`

---

## Schema Endpoints

### List All Schemas

**Endpoint**: `GET /schemas`

**Description**: Returns a list of all available schemas (configuration and CLI).

**Response**:
```json
{
  "success": true,
  "schemas": [
    {
      "name": "AnalysisConfig",
      "type": "configuration",
      "category": "general",
      "jsonSchemaUrl": "/api/v1/schemas/AnalysisConfig",
      "uiSchemaUrl": "/api/v1/schemas/AnalysisConfig/ui",
      "validationUrl": "/api/v1/schemas/AnalysisConfig/validate"
    }
  ],
  "count": 50
}
```

### Get Schema

**Endpoint**: `GET /schemas/{componentName}`

**Description**: Returns the JSON Schema for a configuration component.

**Path Parameters**:
- `componentName` (string, required) - Name of the configuration component (e.g., "AnalysisConfig")

**Response**:
```json
{
  "success": true,
  "schema": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://iped.digital-forensics.org/schema/AnalysisConfig.schema.json",
    "title": "Analysis Configuration",
    "properties": { ... }
  },
  "uiSchema": {
    "property1": {
      "ui:widget": "text",
      "ui:title": "Property 1"
    }
  }
}
```

### Get UI Schema

**Endpoint**: `GET /schemas/{componentName}/ui`

**Description**: Returns the UI Schema for form rendering.

**Path Parameters**:
- `componentName` (string, required) - Name of the configuration component

**Response**:
```json
{
  "success": true,
  "uiSchema": {
    "propertyName": {
      "ui:widget": "text",
      "ui:title": "Human Readable Title",
      "ui:help": "Help text for users"
    }
  }
}
```

### Validate Configuration

**Endpoint**: `POST /schemas/{componentName}/validate`

**Description**: Validates a configuration against its schema.

**Path Parameters**:
- `componentName` (string, required) - Name of the configuration component

**Request Body**:
```json
{
  "config": {
    "property1": "value1",
    "property2": 42
  }
}
```

**Response (Valid)**:
```json
{
  "success": true,
  "valid": true,
  "errors": []
}
```

**Response (Invalid)**:
```json
{
  "success": true,
  "valid": false,
  "errors": [
    "property1: Value must be a string",
    "property2: Required field missing"
  ],
  "errorReport": "Validation failed with 2 error(s)..."
}
```

### List CLI Schemas

**Endpoint**: `GET /cli-schemas`

**Description**: Returns all CLI application schemas.

**Response**:
```json
{
  "success": true,
  "schemas": [
    {
      "name": "IPEDProcessingCLI",
      "type": "cli",
      "category": "cli",
      "jsonSchemaUrl": "/api/v1/schemas/IPEDProcessingCLI"
    }
  ],
  "count": 3
}
```

### Get Schemas by Category

**Endpoint**: `GET /schemas/category/{category}`

**Description**: Returns all schemas in a specific category.

**Path Parameters**:
- `category` (string, required) - Schema category (e.g., "configuration", "cli", "general")

**Response**:
```json
{
  "success": true,
  "schemas": [ ... ],
  "category": "general",
  "count": 5
}
```

---

## Configuration Endpoints

### Get Current Configuration

**Endpoint**: `GET /configurations`

**Description**: Returns all currently loaded configurations.

**Response**:
```json
{
  "success": true,
  "configurations": {
    "AnalysisConfig": {
      "embedLibreOffice": true,
      "searchThreads": 4
    },
    "OCRConfig": {
      "enableOCR": false
    }
  },
  "count": 25
}
```

### Get Specific Configuration

**Endpoint**: `GET /configurations/{componentName}`

**Description**: Returns a specific configuration.

**Path Parameters**:
- `componentName` (string, required) - Name of the configuration component

**Response**:
```json
{
  "success": true,
  "name": "AnalysisConfig",
  "data": {
    "embedLibreOffice": true,
    "searchThreads": 4,
    "maxBackups": 5
  }
}
```

### Export Configuration

**Endpoint**: `GET /configurations/{componentName}/export`

**Description**: Exports a configuration as JSON.

**Path Parameters**:
- `componentName` (string, required) - Name of the configuration component

**Response**:
```json
{
  "success": true,
  "componentName": "AnalysisConfig",
  "json": "{\"embedLibreOffice\": true, \"searchThreads\": 4}",
  "timestamp": 1685336400000
}
```

### Export All Configurations

**Endpoint**: `GET /configurations/export/all`

**Description**: Exports all configurations as JSON.

**Response**:
```json
{
  "success": true,
  "json": "{\"AnalysisConfig\": {...}, \"OCRConfig\": {...}}",
  "count": 25,
  "timestamp": 1685336400000
}
```

### Get Configuration Metadata

**Endpoint**: `GET /configurations/metadata`

**Description**: Returns metadata about all configurations.

**Response**:
```json
{
  "success": true,
  "configurations": [
    {
      "name": "AnalysisConfig",
      "className": "iped.engine.config.AnalysisConfig",
      "packageName": "iped.engine.config",
      "timestamp": 1685336400000
    }
  ],
  "count": 25
}
```

### Create Backup

**Endpoint**: `POST /configurations/backup`

**Description**: Creates a backup of all configurations.

**Request Body**:
```json
{
  "backupPath": "/path/to/backups"
}
```

**Response**:
```json
{
  "success": true,
  "backupId": "550e8400-e29b-41d4-a716-446655440000",
  "backupPath": "/path/to/backups/550e8400-e29b-41d4-a716-446655440000.json",
  "timestamp": 1685336400000
}
```

---

## Configuration Diff & Merge Endpoints

### Compare Configurations

**Endpoint**: `POST /configurations/diff`

**Description**: Calculates the difference between two configurations.

**Request Body**:
```json
{
  "config1": { "field1": "value1", "field2": 42 },
  "config2": { "field1": "value1", "field2": 100 }
}
```

**Response**:
```json
{
  "success": true,
  "added": {},
  "removed": {},
  "modified": {
    "field2": {
      "oldValue": 42,
      "newValue": 100
    }
  },
  "totalChanges": 1
}
```

### Merge Configurations

**Endpoint**: `POST /configurations/merge`

**Description**: Merges two configurations with conflict detection.

**Request Body**:
```json
{
  "baseConfig": { "field1": "base", "field2": 1, "field3": "base" },
  "config1": { "field1": "config1", "field2": 1, "field3": "base" },
  "config2": { "field1": "base", "field2": 2, "field3": "config2" }
}
```

**Response (No Conflicts)**:
```json
{
  "success": true,
  "mergedConfig": {
    "field1": "config1",
    "field2": 2,
    "field3": "config2"
  },
  "applied": { ... },
  "conflicts": {},
  "hasConflicts": false
}
```

**Response (With Conflicts)**:
```json
{
  "success": true,
  "mergedConfig": { ... },
  "applied": { ... },
  "conflicts": {
    "field1": {
      "value1": "config1",
      "value2": "config2"
    }
  },
  "hasConflicts": true,
  "conflictCount": 1
}
```

---

## Error Responses

All endpoints follow a consistent error response format:

```json
{
  "success": false,
  "error": "Descriptive error message"
}
```

**Common HTTP Status Codes**:
- `200 OK` - Request successful
- `400 Bad Request` - Invalid request parameters
- `404 Not Found` - Resource not found (schema, configuration)
- `500 Internal Server Error` - Server error during processing

---

## Usage Examples

### Example 1: Get and Validate Configuration

```bash
# Get the AnalysisConfig schema
curl http://localhost:8080/api/v1/schemas/AnalysisConfig

# Validate a configuration
curl -X POST http://localhost:8080/api/v1/schemas/AnalysisConfig/validate \
  -H "Content-Type: application/json" \
  -d '{
    "config": {
      "embedLibreOffice": true,
      "searchThreads": 4
    }
  }'
```

### Example 2: Export and Backup

```bash
# Export all configurations
curl http://localhost:8080/api/v1/configurations/export/all > backup.json

# Create a timestamped backup
curl -X POST http://localhost:8080/api/v1/configurations/backup \
  -H "Content-Type: application/json" \
  -d '{"backupPath": "/backups"}'
```

### Example 3: Merge Configurations

```bash
curl -X POST http://localhost:8080/api/v1/configurations/merge \
  -H "Content-Type: application/json" \
  -d '{
    "baseConfig": {"threads": 4, "memory": 8},
    "config1": {"threads": 8, "memory": 8},
    "config2": {"threads": 4, "memory": 16}
  }'
```

---

## Server Implementation

The API is implemented using:
- **JAX-RS** (Jersey) - REST framework
- **Embedded Jetty** - HTTP server
- **Jackson** - JSON serialization

### Starting the Server

```bash
# Run with default port (8080)
java -cp iped.jar iped.engine.config.api.ConfigurationServer

# Run with custom port
java -cp iped.jar iped.engine.config.api.ConfigurationServer 9090
```

The server will listen on `http://localhost:8080/api/v1`

---

## Authentication

Currently, the API does not require authentication. For production deployments, consider adding:

- API Key authentication via servlet filters
- JWT token authentication via request interceptors
- Custom security filters

---

## Rate Limiting

No rate limiting is currently implemented. For production, consider:

- Adding servlet filters for per-IP rate limiting
- Implementing custom request interceptors
- Using external rate limiting proxies (e.g., nginx, HAProxy)

---

## API Versioning

The API uses version 1 (`/api/v1`). Future versions will use `/api/v2`, etc.

Breaking changes will trigger a new version. Non-breaking additions will be added to the current version.

---

## See Also

- [SCHEMA-TOOLS.md](SCHEMA-TOOLS.md) - Schema command-line tools
- [CLI-REFERENCE.md](CLI-REFERENCE.md) - CLI argument reference
- [SCHEMA_REGISTRY.md](SCHEMA_REGISTRY.md) - Configuration schema reference
