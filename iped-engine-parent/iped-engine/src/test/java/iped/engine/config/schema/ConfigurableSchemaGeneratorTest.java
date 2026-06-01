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

import com.fasterxml.jackson.databind.node.ObjectNode;
import iped.engine.config.AnalysisConfig;
import iped.engine.config.OCRConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigurableSchemaGenerator.
 */
class ConfigurableSchemaGeneratorTest {

    private iped.engine.config.schema.ConfigurableSchemaGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ConfigurableSchemaGenerator();
    }

    @Test
    void testGenerateBasicJsonSchema() {
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo("TestConfig", "com.example.TestConfig");
        info.setDescription("Test configuration component");

        ConfigurableProperty prop1 = new ConfigurableProperty("enabled", "boolean");
        prop1.setDescription("Enable the feature");
        prop1.setRequired(false);

        ConfigurableProperty prop2 = new ConfigurableProperty("name", "string");
        prop2.setDescription("Component name");
        prop2.setRequired(true);

        info.addProperty(prop1);
        info.addProperty(prop2);

        ObjectNode schema = generator.generateJsonSchema(info);

        assertNotNull(schema);
        assertTrue(schema.has("$schema"));
        assertTrue(schema.has("$id"));
        assertTrue(schema.has("title"));
        assertTrue(schema.has("description"));
        assertTrue(schema.has("type"));
        assertTrue(schema.has("properties"));
        assertTrue(schema.has("required"));
        assertEquals("object", schema.get("type").asText());
    }

    @Test
    void testGenerateJsonSchemaWithProperties() {
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo("ConfigWithProps", "com.example.ConfigWithProps");

        ConfigurableProperty stringProp = new ConfigurableProperty("hostname", "string");
        stringProp.setDescription("Server hostname");

        ConfigurableProperty intProp = new ConfigurableProperty("port", "integer");
        intProp.setDescription("Server port");
        intProp.setDefaultValue(8080);

        ConfigurableProperty boolProp = new ConfigurableProperty("ssl", "boolean");
        boolProp.setDescription("Use SSL");
        boolProp.setDefaultValue(false);

        info.addProperty(stringProp);
        info.addProperty(intProp);
        info.addProperty(boolProp);

        ObjectNode schema = generator.generateJsonSchema(info);

        assertTrue(schema.has("properties"));
        ObjectNode properties = (ObjectNode) schema.get("properties");
        assertTrue(properties.has("hostname"));
        assertTrue(properties.has("port"));
        assertTrue(properties.has("ssl"));

        assertEquals("string", properties.get("hostname").get("type").asText());
        assertEquals("integer", properties.get("port").get("type").asText());
        assertEquals("boolean", properties.get("ssl").get("type").asText());
    }

    @Test
    void testGenerateJsonSchemaWithRequired() {
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo("RequiredPropsConfig", "com.example.RequiredPropsConfig");

        ConfigurableProperty required1 = new ConfigurableProperty("username", "string");
        required1.setDescription("Username");
        required1.setRequired(true);

        ConfigurableProperty optional1 = new ConfigurableProperty("nickname", "string");
        optional1.setDescription("Nickname");
        optional1.setRequired(false);

        info.addProperty(required1);
        info.addProperty(optional1);

        ObjectNode schema = generator.generateJsonSchema(info);

        assertTrue(schema.has("required"));
        String requiredField = schema.get("required").get(0).asText();
        assertEquals("username", requiredField);
    }

    @Test
    void testGenerateUiSchema() {
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo("UITestConfig", "com.example.UITestConfig");

        ConfigurableProperty stringProp = new ConfigurableProperty("setting1", "string");
        stringProp.setDescription("A string setting");

        ConfigurableProperty intProp = new ConfigurableProperty("setting2", "integer");
        intProp.setDescription("An integer setting");

        ConfigurableProperty boolProp = new ConfigurableProperty("setting3", "boolean");
        boolProp.setDescription("A boolean setting");

        info.addProperty(stringProp);
        info.addProperty(intProp);
        info.addProperty(boolProp);

        ObjectNode uiSchema = generator.generateUiSchema(info);

        assertNotNull(uiSchema);
        assertTrue(uiSchema.has("setting1"));
        assertTrue(uiSchema.has("setting2"));
        assertTrue(uiSchema.has("setting3"));

        // Check UI widgets
        ObjectNode intField = (ObjectNode) uiSchema.get("setting2");
        assertEquals("updown", intField.get("ui:widget").asText());

        ObjectNode boolField = (ObjectNode) uiSchema.get("setting3");
        assertEquals("checkbox", boolField.get("ui:widget").asText());
    }

    @Test
    void testGeneratePropertySchemaWithEnumValues() {
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo("EnumConfig", "com.example.EnumConfig");

        ConfigurableProperty enumProp = new ConfigurableProperty("profile", "string");
        enumProp.setDescription("Processing profile");
        enumProp.setEnumValues(java.util.Arrays.asList("forensic", "pedo", "fastmode"));

        info.addProperty(enumProp);

        ObjectNode schema = generator.generateJsonSchema(info);

        ObjectNode properties = (ObjectNode) schema.get("properties");
        ObjectNode profileProp = (ObjectNode) properties.get("profile");
        assertTrue(profileProp.has("enum"));
        assertEquals(3, profileProp.get("enum").size());
    }

    @Test
    void testAnalyzeConfigurable() {
        ConfigurableSchemaInfo info = generator.analyzeConfigurable(AnalysisConfig.class);

        assertNotNull(info);
        assertEquals("AnalysisConfig", info.getComponentName());
        assertTrue(info.getConfigurableClassName().contains("AnalysisConfig"));
    }

    @Test
    void testAnalyzeAnotherConfigurable() {
        ConfigurableSchemaInfo info = generator.analyzeConfigurable(OCRConfig.class);

        assertNotNull(info);
        assertEquals("OCRConfig", info.getComponentName());
        assertTrue(info.getConfigurableClassName().contains("OCRConfig"));
    }

    @Test
    void testSchemaJsonSchemaVersion() {
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo("VersionTest", "com.example.VersionTest");
        ObjectNode schema = generator.generateJsonSchema(info);

        assertTrue(schema.has("$schema"));
        String schemaVersion = schema.get("$schema").asText();
        assertTrue(schemaVersion.contains("json-schema.org"));
        assertTrue(schemaVersion.contains("2020-12"));
    }

    @Test
    void testSchemaIdGeneration() {
        ConfigurableSchemaInfo info = new ConfigurableSchemaInfo("MyComponent", "com.example.MyComponent");
        ObjectNode schema = generator.generateJsonSchema(info);

        assertTrue(schema.has("$id"));
        String id = schema.get("$id").asText();
        assertTrue(id.contains("iped.digital-forensics.org"));
        assertTrue(id.contains("MyComponent"));
        assertTrue(id.endsWith(".schema.json"));
    }

}
