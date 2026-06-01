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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaAPIControllerTest {
    private iped.engine.config.api.SchemaAPIController controller;

    @BeforeEach
    public void setUp() {
        controller = new SchemaAPIController();
    }

    @Test
    public void testListSchemasReturnsNonEmpty() {
        List<Map<String, Object>> schemas = controller.listSchemas();

        assertNotNull(schemas, "Schemas list should not be null");
        assertFalse(schemas.isEmpty(), "Schemas list should not be empty");
        assertTrue(schemas.size() >= 49, "Should have at least 49 schemas");
    }

    @Test
    public void testListSchemasHasRequiredFields() {
        List<Map<String, Object>> schemas = controller.listSchemas();
        Map<String, Object> firstSchema = schemas.get(0);

        assertTrue(firstSchema.containsKey("name"), "Schema should have name");
        assertTrue(firstSchema.containsKey("type"), "Schema should have type");
        assertTrue(firstSchema.containsKey("category"), "Schema should have category");
        assertTrue(firstSchema.containsKey("jsonSchemaUrl"), "Schema should have jsonSchemaUrl");
    }

    @Test
    public void testListSchemasContainsConfigurationSchemas() {
        List<Map<String, Object>> schemas = controller.listSchemas();

        boolean hasAnalysisConfig = schemas.stream()
            .anyMatch(s -> "AnalysisConfig".equals(s.get("name")));
        assertTrue(hasAnalysisConfig, "Should contain AnalysisConfig schema");

        boolean hasTaskInstaller = schemas.stream()
            .anyMatch(s -> "TaskInstallerConfig".equals(s.get("name")));
        assertTrue(hasTaskInstaller, "Should contain TaskInstallerConfig schema");
    }

    @Test
    public void testListSchemasContainsCLISchemas() {
        List<Map<String, Object>> schemas = controller.listSchemas();

        boolean hasCLI = schemas.stream()
            .anyMatch(s -> s.get("name").toString().contains("CLI"));
        assertTrue(hasCLI, "Should contain at least one CLI schema");
    }

    @Test
    public void testGetSchemaReturnsValidResult() {
        Map<String, Object> result = controller.getSchema("AnalysisConfig");

        assertTrue((boolean) result.get("success"), "Should be successful");
        assertTrue(result.containsKey("schema"), "Should contain schema");
    }

    @Test
    public void testGetSchemaReturnsError() {
        Map<String, Object> result = controller.getSchema("NonExistentConfig");

        assertFalse((boolean) result.get("success"), "Should fail for nonexistent schema");
        assertTrue(result.containsKey("error"), "Should contain error message");
    }

    @Test
    public void testGetCLISchemaReturnsValidResult() {
        Map<String, Object> result = controller.getCLISchema("IPEDProcessingCLI");

        assertTrue((boolean) result.get("success"), "Should be successful");
        assertTrue(result.containsKey("schema"), "Should contain schema");
    }

    @Test
    public void testValidateConfigurationWithValidConfig() {
        String validConfig = "{\"threads\": 4, \"embedLibreOffice\": true}";
        Map<String, Object> result = controller.validateConfiguration("AnalysisConfig", validConfig);

        assertTrue((boolean) result.get("success"), "Should be successful");
    }

    @Test
    public void testValidateConfigurationWithInvalidJson() {
        String invalidJson = "{invalid json";
        Map<String, Object> result = controller.validateConfiguration("AnalysisConfig", invalidJson);

        assertTrue((boolean) result.get("success"), "Response should be successful (error reported in validation result)");
    }

    @Test
    public void testListCLISchemasReturnsThreeSchemas() {
        List<Map<String, Object>> cliSchemas = controller.listCLISchemas();

        assertNotNull(cliSchemas, "CLI schemas should not be null");
        assertEquals(3, cliSchemas.size(), "Should have exactly 3 CLI schemas");
    }

    @Test
    public void testListCLISchemasContainsExpectedApps() {
        List<Map<String, Object>> cliSchemas = controller.listCLISchemas();

        boolean hasProcessing = cliSchemas.stream()
            .anyMatch(s -> "IPEDProcessingCLI".equals(s.get("name")));
        assertTrue(hasProcessing, "Should contain IPEDProcessingCLI");

        boolean hasWebAPI = cliSchemas.stream()
            .anyMatch(s -> "IPEDWebAPICLI".equals(s.get("name")));
        assertTrue(hasWebAPI, "Should contain IPEDWebAPICLI");

        boolean hasSearchApp = cliSchemas.stream()
            .anyMatch(s -> "IPEDSearchAppCLI".equals(s.get("name")));
        assertTrue(hasSearchApp, "Should contain IPEDSearchAppCLI");
    }

    @Test
    public void testGetSchemasByCategory() {
        List<Map<String, Object>> configSchemas = controller.getSchemasByCategory("configuration");

        assertNotNull(configSchemas, "Category schemas should not be null");
        assertFalse(configSchemas.isEmpty(), "Should have configuration category schemas");

        boolean allAreConfiguration = configSchemas.stream()
            .allMatch(s -> "configuration".equals(s.get("category")));
        assertTrue(allAreConfiguration, "All returned schemas should be in configuration category");
    }

    @Test
    public void testGetSchemasByCategoryCLI() {
        List<Map<String, Object>> cliSchemas = controller.getSchemasByCategory("cli");

        assertNotNull(cliSchemas, "CLI category schemas should not be null");
        assertTrue(cliSchemas.size() >= 3, "Should have at least 3 CLI schemas");

        boolean allAreCLI = cliSchemas.stream()
            .allMatch(s -> "cli".equals(s.get("category")));
        assertTrue(allAreCLI, "All returned schemas should be in cli category");
    }

    @Test
    public void testGetSchemasByCategoryEmptyForInvalid() {
        List<Map<String, Object>> schemas = controller.getSchemasByCategory("invalidCategory");

        assertNotNull(schemas, "Should return list (even if empty)");
        assertEquals(0, schemas.size(), "Should return empty list for invalid category");
    }

    @Test
    public void testSchemaMetadataURLsAreFormatted() {
        List<Map<String, Object>> schemas = controller.listSchemas();
        Map<String, Object> schema = schemas.get(0);

        String jsonUrl = (String) schema.get("jsonSchemaUrl");
        String uiUrl = (String) schema.get("uiSchemaUrl");
        String validateUrl = (String) schema.get("validationUrl");

        assertTrue(jsonUrl.startsWith("/api/schemas/"), "JSON schema URL should start with /api/schemas/");
        assertTrue(uiUrl.startsWith("/api/schemas/"), "UI schema URL should start with /api/schemas/");
        assertTrue(validateUrl.startsWith("/api/schemas/"), "Validation URL should start with /api/schemas/");
    }
}
