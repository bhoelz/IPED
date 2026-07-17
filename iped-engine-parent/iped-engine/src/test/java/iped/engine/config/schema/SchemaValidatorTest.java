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

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for SchemaValidator. Tests basic validation functionality. */
class SchemaValidatorTest {

  private SchemaValidator validator;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    validator = new SchemaValidator();
    mapper = new ObjectMapper();
  }

  @Test
  void testValidatorInitialization() {
    assertNotNull(validator);
    assertNotNull(mapper);
  }

  @Test
  void testValidationResultSuccess() {
    SchemaValidator.ValidationResult result =
        new SchemaValidator.ValidationResult(true, new java.util.ArrayList<>());
    assertTrue(result.isValid());
    assertEquals(0, result.getErrors().size());
  }

  @Test
  void testValidationResultFailure() {
    java.util.List<String> errors = new java.util.ArrayList<>();
    errors.add("Test error");
    SchemaValidator.ValidationResult result = new SchemaValidator.ValidationResult(false, errors);

    assertFalse(result.isValid());
    assertEquals(1, result.getErrors().size());
    assertTrue(result.getErrorReport().contains("Validation failed"));
  }

  @Test
  void testValidationResultErrorReporting() {
    java.util.List<String> errors = new java.util.ArrayList<>();
    errors.add("Field 'name' is required");
    errors.add("Field 'age' must be an integer");
    SchemaValidator.ValidationResult result = new SchemaValidator.ValidationResult(false, errors);

    String report = result.getErrorReport();
    assertTrue(report.contains("2 error"));
    assertTrue(report.contains("name"));
    assertTrue(report.contains("age"));
  }

  @Test
  void testValidateEmptyJson() throws Exception {
    String schema = "{ \"type\": \"object\" }";
    String json = "{}";
    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);

    SchemaValidator.ValidationResult result = validator.validateJson(json, schemaNode);
    assertNotNull(result);
  }

  @Test
  void testValidateComplexJson() throws Exception {
    String json = "{ \"name\": \"test\", \"count\": 42, \"enabled\": true }";
    String schema = "{ \"type\": \"object\" }";
    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);

    SchemaValidator.ValidationResult result = validator.validateJson(json, schemaNode);
    assertNotNull(result);
  }

  @Test
  void testValidateWithObjectType() throws Exception {
    String schema = "{ \"type\": \"object\" }";
    String validJson = "{ \"field\": \"value\" }";

    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);
    SchemaValidator.ValidationResult result = validator.validateJson(validJson, schemaNode);

    assertTrue(result.isValid());
  }

  @Test
  void testValidationDoesNotThrow() throws Exception {
    String schema = "{ \"type\": \"object\" }";
    String json = "{ \"test\": \"data\" }";
    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);

    assertDoesNotThrow(
        () -> {
          validator.validateJson(json, schemaNode);
        });
  }
}
