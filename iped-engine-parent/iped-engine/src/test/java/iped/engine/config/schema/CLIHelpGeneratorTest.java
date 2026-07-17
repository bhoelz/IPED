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

/** Unit tests for CLIHelpGenerator. */
class CLIHelpGeneratorTest {

  private CLIHelpGenerator generator;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    generator = new CLIHelpGenerator();
    mapper = new ObjectMapper();
  }

  @Test
  void testGenerateHelpWithSimpleSchema() throws Exception {
    String schema =
        "{\n"
            + "  \"title\": \"Test CLI\",\n"
            + "  \"description\": \"A simple test CLI\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"output\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"description\": \"Output directory\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);
    String help = generator.generateFromSchema(schemaNode);

    assertNotNull(help);
    assertTrue(help.contains("Test CLI"));
    assertTrue(help.contains("A simple test CLI"));
    assertTrue(help.contains("output"));
  }

  @Test
  void testGenerateHelpWithRequiredFields() throws Exception {
    String schema =
        "{\n"
            + "  \"title\": \"Test CLI\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"input\": {\"type\": \"string\", \"description\": \"Input file\"},\n"
            + "    \"output\": {\"type\": \"string\", \"description\": \"Output file\"}\n"
            + "  },\n"
            + "  \"required\": [\"input\"]\n"
            + "}";

    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);
    String help = generator.generateFromSchema(schemaNode);

    assertNotNull(help);
    assertTrue(help.contains("Required arguments"));
    assertTrue(help.contains("input"));
    assertTrue(help.contains("Optional arguments"));
    assertTrue(help.contains("output"));
  }

  @Test
  void testGenerateHelpWithDefaultValues() throws Exception {
    String schema =
        "{\n"
            + "  \"title\": \"Test CLI\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"port\": {\n"
            + "      \"type\": \"integer\",\n"
            + "      \"description\": \"Server port\",\n"
            + "      \"default\": 8080\n"
            + "    }\n"
            + "  }\n"
            + "}";

    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);
    String help = generator.generateFromSchema(schemaNode);

    assertNotNull(help);
    assertTrue(help.contains("Default"));
    assertTrue(help.contains("8080"));
  }

  @Test
  void testGenerateHelpWithEnumValues() throws Exception {
    String schema =
        "{\n"
            + "  \"title\": \"Test CLI\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"mode\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"description\": \"Operation mode\",\n"
            + "      \"enum\": [\"read\", \"write\", \"append\"]\n"
            + "    }\n"
            + "  }\n"
            + "}";

    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);
    String help = generator.generateFromSchema(schemaNode);

    assertNotNull(help);
    assertTrue(help.contains("Valid values"));
    assertTrue(help.contains("read"));
    assertTrue(help.contains("write"));
    assertTrue(help.contains("append"));
  }

  @Test
  void testGenerateHelpForNonexistentSchema() {
    String help = generator.generateHelp("NonexistentSchema");
    assertNotNull(help);
    assertTrue(help.contains("Schema not found") || help.contains("NonexistentSchema"));
  }

  @Test
  void testGenerateAllHelpDoesNotThrow() {
    assertDoesNotThrow(
        () -> {
          String help = generator.generateAllHelp();
          assertNotNull(help);
        });
  }

  @Test
  void testHelpContainsTitleAndDescription() throws Exception {
    String schema =
        "{\n"
            + "  \"title\": \"Custom CLI\",\n"
            + "  \"description\": \"Custom description\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {}\n"
            + "}";

    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);
    String help = generator.generateFromSchema(schemaNode);

    assertTrue(help.contains("Custom CLI"));
    assertTrue(help.contains("Custom description"));
  }

  @Test
  void testHelpFormatIsReadable() throws Exception {
    String schema =
        "{\n"
            + "  \"title\": \"Test\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"arg1\": {\"type\": \"string\", \"description\": \"First argument\"},\n"
            + "    \"arg2\": {\"type\": \"integer\", \"description\": \"Second argument\"}\n"
            + "  }\n"
            + "}";

    ObjectNode schemaNode = (ObjectNode) mapper.readTree(schema);
    String help = generator.generateFromSchema(schemaNode);

    // Check for USAGE section
    assertTrue(help.contains("USAGE"));
    // Check for indentation/formatting
    assertTrue(help.contains("  "));
  }
}
