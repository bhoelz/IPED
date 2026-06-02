package iped.engine.config.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generates JSON Schema and UI Schema for Configurable components.
 * Uses JSON Schema Draft 2020-12 standard for compatibility.
 */
public class ConfigurableSchemaGenerator {
    private static final String JSON_SCHEMA_VERSION = "https://json-schema.org/draft/2020-12/schema";
    private static final String SCHEMA_BASE_URL = "https://iped.digital-forensics.org/schema/";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Generate JSON Schema for a Configurable component.
     *
     * @param info ConfigurableSchemaInfo containing component metadata
     * @return JSON Schema as ObjectNode (can be converted to String)
     */
    public ObjectNode generateJsonSchema(iped.engine.config.schema.ConfigurableSchemaInfo info) {
        ObjectNode schema = mapper.createObjectNode();

        schema.put("$schema", JSON_SCHEMA_VERSION);
        schema.put("$id", SCHEMA_BASE_URL + info.getComponentName() + ".schema.json");
        schema.put("title", info.getComponentName());
        if (info.getDescription() != null) {
            schema.put("description", info.getDescription());
        }
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        ObjectNode required = mapper.createObjectNode();
        ArrayNode requiredArray = mapper.createArrayNode();

        for (ConfigurableProperty prop : info.getProperties()) {
            ObjectNode propSchema = generatePropertySchema(prop);
            properties.set(prop.getName(), propSchema);

            if (prop.isRequired()) {
                requiredArray.add(prop.getName());
            }
        }

        schema.set("properties", properties);
        if (requiredArray.size() > 0) {
            schema.set("required", requiredArray);
        }

        schema.put("additionalProperties", false);

        return schema;
    }

    /**
     * Generate UI Schema for a Configurable component.
     * Uses JSON Form / react-jsonschema-form conventions.
     *
     * @param info ConfigurableSchemaInfo containing component metadata
     * @return UI Schema as ObjectNode
     */
    public ObjectNode generateUiSchema(ConfigurableSchemaInfo info) {
        ObjectNode uiSchema = mapper.createObjectNode();

        for (ConfigurableProperty prop : info.getProperties()) {
            ObjectNode propUiSchema = generatePropertyUiSchema(prop);
            uiSchema.set(prop.getName(), propUiSchema);
        }

        return uiSchema;
    }

    /**
     * Generate schema for a single property.
     */
    private ObjectNode generatePropertySchema(ConfigurableProperty prop) {
        ObjectNode propSchema = mapper.createObjectNode();

        String type = prop.getType();
        switch (type.toLowerCase()) {
            case "string":
                propSchema.put("type", "string");
                if (prop.getPattern() != null) {
                    propSchema.put("pattern", prop.getPattern());
                }
                break;
            case "integer":
            case "int":
                propSchema.put("type", "integer");
                break;
            case "boolean":
            case "bool":
                propSchema.put("type", "boolean");
                break;
            case "number":
                propSchema.put("type", "number");
                break;
            case "array":
                propSchema.put("type", "array");
                ObjectNode items = mapper.createObjectNode();
                items.put("type", "string");
                propSchema.set("items", items);
                break;
            case "object":
                propSchema.put("type", "object");
                break;
            default:
                propSchema.put("type", "string");
        }

        if (prop.getDescription() != null) {
            propSchema.put("description", prop.getDescription());
        }

        if (prop.getDefaultValue() != null) {
            propSchema.putPOJO("default", prop.getDefaultValue());
        }

        if (prop.getEnumValues() != null && !prop.getEnumValues().isEmpty()) {
            ArrayNode enumArray = mapper.createArrayNode();
            for (String enumVal : prop.getEnumValues()) {
                enumArray.add(enumVal);
            }
            propSchema.set("enum", enumArray);
        }

        return propSchema;
    }

    /**
     * Generate UI schema for a single property.
     */
    private ObjectNode generatePropertyUiSchema(ConfigurableProperty prop) {
        ObjectNode propUiSchema = mapper.createObjectNode();

        if (prop.getDescription() != null) {
            propUiSchema.put("ui:help", prop.getDescription());
        }

        String type = prop.getType();
        switch (type.toLowerCase()) {
            case "string":
                if (prop.getDescription() != null && prop.getDescription().contains("URL")) {
                    propUiSchema.put("ui:widget", "url");
                } else if (prop.getDescription() != null &&
                          (prop.getDescription().contains("pattern") ||
                           prop.getDescription().contains("regex"))) {
                    propUiSchema.put("ui:widget", "textarea");
                }
                break;
            case "integer":
            case "int":
                propUiSchema.put("ui:widget", "updown");
                break;
            case "boolean":
            case "bool":
                propUiSchema.put("ui:widget", "checkbox");
                break;
            case "array":
                ObjectNode arrayUiSchema = mapper.createObjectNode();
                arrayUiSchema.put("ui:help", "Add items to the list");
                propUiSchema.set("items", arrayUiSchema);
                break;
        }

        return propUiSchema;
    }

    /**
     * Analyze a Configurable class and extract its schema information.
     * This is a helper method that can be overridden for specific component types.
     *
     * @param configurableClass The Configurable class to analyze
     * @return ConfigurableSchemaInfo with extracted metadata
     */
    public ConfigurableSchemaInfo analyzeConfigurable(Class<?> configurableClass) {
        String className = configurableClass.getSimpleName();
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo(className, configurableClass.getName());

        // Default configuration type is String
        info.setConfigurationTypeGeneric("String");

        // Try to extract generic type from class declaration
        try {
            java.lang.reflect.Type[] types = configurableClass.getGenericInterfaces();
            for (java.lang.reflect.Type type : types) {
                String typeStr = type.toString();
                if (typeStr.contains("Configurable")) {
                    int startIdx = typeStr.lastIndexOf("<") + 1;
                    int endIdx = typeStr.lastIndexOf(">");
                    if (startIdx > 0 && endIdx > startIdx) {
                        String genericType = typeStr.substring(startIdx, endIdx);
                        info.setConfigurationTypeGeneric(genericType);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore, use default
        }

        return info;
    }
}
