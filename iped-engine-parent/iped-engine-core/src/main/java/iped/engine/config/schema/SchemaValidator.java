package iped.engine.config.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates configurations against their JSON schemas.
 * Provides detailed validation error messages.
 */
public class SchemaValidator {
    private final ObjectMapper mapper = new ObjectMapper()
            // Some Configurables expose third-party object graphs (e.g. Tika's MediaType,
            // whose internal baseType field points back to itself for parameterless types)
            // that aren't real JSON cycles but trip Jackson's bean-serialization recursion
            // guard. Serializing them as their string form matches what every schema that
            // references them ("type": "string") actually expects anyway.
            .registerModule(new SimpleModule().addSerializer(org.apache.tika.mime.MediaType.class, ToStringSerializer.instance))
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    /**
     * Validate a configuration object against its schema.
     *
     * @param config The configuration object to validate
     * @param schema The JSON Schema to validate against
     * @return ValidationResult containing success status and any errors
     */
    public ValidationResult validate(Object config, ObjectNode schema) {
        List<String> errors = new ArrayList<>();

        try {
            JsonNode configNode = mapper.valueToTree(config);
            validateNode(configNode, schema, "", errors);

            return new ValidationResult(errors.isEmpty(), errors);
        } catch (Exception e) {
            errors.add("Error during validation: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validate a JSON string against a schema.
     *
     * @param jsonString The JSON string to validate
     * @param schema The JSON Schema to validate against
     * @return ValidationResult containing success status and any errors
     */
    public ValidationResult validateJson(String jsonString, ObjectNode schema) {
        List<String> errors = new ArrayList<>();

        try {
            JsonNode configNode = mapper.readTree(jsonString);
            validateNode(configNode, schema, "", errors);

            return new ValidationResult(errors.isEmpty(), errors);
        } catch (IOException e) {
            errors.add("Invalid JSON: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Internal method to recursively validate a JSON node.
     */
    private void validateNode(JsonNode node, JsonNode schema, String path, List<String> errors) {
        if (schema == null) {
            return;
        }

        if (schema.has("type")) {
            String expectedType = schema.get("type").asText();
            if (!validateType(node, expectedType)) {
                errors.add(String.format("%s: Expected type '%s' but got '%s'",
                    path.isEmpty() ? "root" : path, expectedType, getNodeType(node)));
            }
        }

        if (schema.has("enum")) {
            boolean found = false;
            for (JsonNode enumValue : schema.get("enum")) {
                if (node.equals(enumValue)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errors.add(String.format("%s: Value '%s' is not one of the allowed values",
                    path.isEmpty() ? "root" : path, node.asText()));
            }
        }

        if (schema.has("pattern") && node.isTextual()) {
            String pattern = schema.get("pattern").asText();
            if (!node.asText().matches(pattern)) {
                errors.add(String.format("%s: Value '%s' does not match pattern '%s'",
                    path.isEmpty() ? "root" : path, node.asText(), pattern));
            }
        }

        if (schema.has("properties") && node.isObject()) {
            JsonNode properties = schema.get("properties");
            for (JsonNode property : properties) {
                String propName = property.fieldNames().next();
                if (node.has(propName)) {
                    String newPath = path.isEmpty() ? propName : path + "." + propName;
                    validateNode(node.get(propName), properties.get(propName), newPath, errors);
                }
            }
        }

        if (schema.has("required")) {
            JsonNode required = schema.get("required");
            if (node.isObject()) {
                for (JsonNode req : required) {
                    String requiredField = req.asText();
                    if (!node.has(requiredField)) {
                        errors.add(String.format("%s: Required field '%s' is missing",
                            path.isEmpty() ? "root" : path, requiredField));
                    }
                }
            }
        }
    }

    private boolean validateType(JsonNode node, String expectedType) {
        switch (expectedType) {
            case "string":
                return node.isTextual();
            case "integer":
            case "int":
                return node.isIntegralNumber();
            case "number":
                return node.isNumber();
            case "boolean":
                return node.isBoolean();
            case "array":
                return node.isArray();
            case "object":
                return node.isObject();
            case "null":
                return node.isNull();
            default:
                return true;
        }
    }

    private String getNodeType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isIntegralNumber()) return "integer";
        if (node.isNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        if (node.isNull()) return "null";
        return "unknown";
    }

    /**
     * Result of schema validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorReport() {
            if (valid) {
                return "Validation successful";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Validation failed with ").append(errors.size()).append(" error(s):\n");
            for (String error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
            return sb.toString();
        }
    }
}
