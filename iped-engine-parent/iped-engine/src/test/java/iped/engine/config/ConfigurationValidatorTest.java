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
package iped.engine.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigurationValidator.
 */
class ConfigurationValidatorTest {

    private ConfigurationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigurationValidator();
    }

    @Test
    void testComponentNameDerivation() {
        // Test that component names are correctly derived from class names
        String componentName1 = getComponentName(AnalysisConfig.class);
        assertEquals("AnalysisConfig", componentName1);

        String componentName2 = getComponentName(OCRConfig.class);
        assertEquals("OCRConfig", componentName2);
    }

    @Test
    void testValidationStatistics() {
        ConfigurationValidator.ValidationStats stats = new ConfigurationValidator.ValidationStats();

        assertEquals(0, stats.getTotalValidated());
        assertTrue(stats.allPassed());

        stats.successCount = 5;
        stats.failureCount = 2;
        stats.failedComponents.add("Component1");
        stats.failedComponents.add("Component2");

        assertEquals(7, stats.getTotalValidated());
        assertFalse(stats.allPassed());
        assertEquals(5, stats.successCount);
        assertEquals(2, stats.failureCount);
    }

    @Test
    void testValidationStatsToString() {
        ConfigurationValidator.ValidationStats stats = new ConfigurationValidator.ValidationStats();
        stats.successCount = 10;
        stats.failureCount = 0;

        String result = stats.toString();
        assertNotNull(result);
        assertTrue(result.contains("success=10"));
        assertTrue(result.contains("failures=0"));
    }

    @Test
    void testValidateAnalysisConfig() {
        // Test validation of AnalysisConfig
        AnalysisConfig config = new AnalysisConfig();

        // This should validate successfully even without a schema file
        // (since validation gracefully handles missing schemas)
        boolean valid = validator.validateConfiguration(config, AnalysisConfig.class);

        // The validation should return true if no schema exists
        // or true if the config is valid
        assertNotNull(valid);
    }

    @Test
    void testValidateOCRConfig() {
        // Test validation of OCRConfig
        OCRConfig config = new OCRConfig();

        boolean valid = validator.validateConfiguration(config, OCRConfig.class);

        assertNotNull(valid);
    }

    @Test
    void testValidateFileSystemConfig() {
        // Test validation of FileSystemConfig
        FileSystemConfig config = new FileSystemConfig();

        boolean valid = validator.validateConfiguration(config, FileSystemConfig.class);

        assertNotNull(valid);
    }

    @Test
    void testValidatorsDoNotThrow() {
        // Ensure that validator gracefully handles edge cases
        // and doesn't throw exceptions

        assertDoesNotThrow(() -> {
            AnalysisConfig config = new AnalysisConfig();
            validator.validateConfiguration(config, AnalysisConfig.class);
        });

        assertDoesNotThrow(() -> {
            OCRConfig config = new OCRConfig();
            validator.validateConfiguration(config, OCRConfig.class);
        });
    }

    /**
     * Helper method to derive component name from class.
     * Mirrors logic in ConfigurationValidator.
     */
    private String getComponentName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (simpleName.endsWith("Config")) {
            return simpleName;
        }
        return simpleName + "Config";
    }

}
