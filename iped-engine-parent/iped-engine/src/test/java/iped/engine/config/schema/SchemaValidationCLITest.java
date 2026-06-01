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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SchemaValidationCLI.
 */
class SchemaValidationCLITest {

    private iped.engine.config.schema.SchemaValidationCLI cli;

    @BeforeEach
    void setUp() {
        cli = new SchemaValidationCLI();
    }

    @Test
    void testValidateConfigSchema() {
        assertDoesNotThrow(() -> {
            cli.validateConfigSchema("AnalysisConfig");
        });
    }

    @Test
    void testValidateCLISchema() {
        assertDoesNotThrow(() -> {
            cli.validateCLISchema("IPEDProcessingCLI");
        });
    }

    @Test
    void testValidateNonexistentConfigSchema() {
        assertDoesNotThrow(() -> {
            cli.validateConfigSchema("NonexistentConfig");
        });
    }

    @Test
    void testValidateAllSchemas() {
        assertDoesNotThrow(() -> {
            cli.validateAllSchemas();
        });
    }

    @Test
    void testValidateMultipleSchemas() {
        assertDoesNotThrow(() -> {
            cli.validateConfigSchema("AnalysisConfig");
            cli.validateConfigSchema("OCRConfig");
            cli.validateConfigSchema("FileSystemConfig");
            cli.validateCLISchema("IPEDWebAPICLI");
        });
    }

    @Test
    void testValidationDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            cli.validateConfigSchema("AnalysisConfig");
            cli.validateCLISchema("IPEDProcessingCLI");
            cli.validateAllSchemas();
        });
    }

}
