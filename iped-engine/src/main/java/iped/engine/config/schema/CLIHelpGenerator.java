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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Generates CLI help text from JSON schemas.
 * Produces formatted help documentation for command-line arguments.
 */
public class CLIHelpGenerator {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final int HELP_INDENT = 2;
    private static final int HELP_WRAP = 80;

    /**
     * Generate help text from a CLI schema.
     *
     * @param schemaName Name of the schema (e.g., "IPEDProcessingCLI")
     * @return Formatted help text
     */
    public String generateHelp(String schemaName) {
        try {
            ObjectNode schema = loadSchema(schemaName);
            if (schema == null) {
                return "Schema not found: " + schemaName;
            }

            return generateFromSchema(schema);
        } catch (IOException e) {
            return "Error generating help: " + e.getMessage();
        }
    }

    /**
     * Generate help text from a schema object.
     *
     * @param schema The JSON schema
     * @return Formatted help text
     */
    public String generateFromSchema(JsonNode schema) {
        StringBuilder help = new StringBuilder();

        // Title and description
        if (schema.has("title")) {
            help.append(schema.get("title").asText()).append("\n");
        }
        if (schema.has("description")) {
            help.append("\n").append(schema.get("description")).append("\n");
        }

        help.append("\n").append("USAGE:\n");

        // Required arguments
        List<String> required = new ArrayList<>();
        if (schema.has("required")) {
            schema.get("required").forEach(node -> required.add(node.asText()));
        }

        if (!required.isEmpty()) {
            help.append("  Required arguments:\n");
            for (String arg : required) {
                help.append(formatArgument(schema, arg, true));
            }
        }

        // Optional arguments
        List<String> optional = new ArrayList<>();
        if (schema.has("properties")) {
            JsonNode props = schema.get("properties");
            for (Iterator<String> it = props.fieldNames(); it.hasNext(); ) {
                String fieldName = it.next();
                if (!required.contains(fieldName)) {
                    optional.add(fieldName);
                }
            }
        }

        if (!optional.isEmpty()) {
            help.append("\n  Optional arguments:\n");
            for (String arg : optional) {
                help.append(formatArgument(schema, arg, false));
            }
        }

        return help.toString();
    }

    /**
     * Format a single argument for help display.
     */
    private String formatArgument(JsonNode schema, String fieldName, boolean required) {
        StringBuilder sb = new StringBuilder();

        JsonNode prop = schema.get("properties").get(fieldName);
        String type = prop.has("type") ? prop.get("type").asText() : "string";
        String description = prop.has("description") ? prop.get("description").asText() : "No description";

        sb.append("    ");
        sb.append(String.format("%-20s", fieldName));
        sb.append("  ");
        sb.append(String.format("%-15s", "[" + type + "]"));
        if (required) {
            sb.append(" (required)");
        }
        sb.append("\n");

        // Description with wrapping
        String[] words = description.split("\\s+");
        int lineLength = 20 + 2 + 15 + 5;
        StringBuilder descLine = new StringBuilder();

        for (String word : words) {
            if (descLine.length() + word.length() + 1 > (HELP_WRAP - lineLength)) {
                if (descLine.length() > 0) {
                    sb.append("      ").append(descLine).append("\n");
                    descLine = new StringBuilder();
                }
            }
            if (descLine.length() > 0) {
                descLine.append(" ");
            }
            descLine.append(word);
        }

        if (descLine.length() > 0) {
            sb.append("      ").append(descLine).append("\n");
        }

        // Default value
        if (prop.has("default")) {
            String defaultVal = prop.get("default").asText();
            sb.append("      Default: ").append(defaultVal).append("\n");
        }

        // Enum values
        if (prop.has("enum")) {
            sb.append("      Valid values: ");
            List<String> values = new ArrayList<>();
            prop.get("enum").forEach(node -> values.add(node.asText()));
            sb.append(String.join(", ", values)).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Load a CLI schema from resources.
     *
     * @param schemaName Name of the schema
     * @return The loaded schema, or null if not found
     */
    private ObjectNode loadSchema(String schemaName) throws IOException {
        String path = "/schemas/json/" + schemaName + ".schema.json";
        InputStream stream = getClass().getResourceAsStream(path);

        if (stream == null) {
            return null;
        }

        try {
            return (ObjectNode) mapper.readTree(stream);
        } finally {
            stream.close();
        }
    }

    /**
     * Generate help for all CLI schemas.
     *
     * @return Combined help text for all CLIs
     */
    public String generateAllHelp() {
        StringBuilder allHelp = new StringBuilder();
        allHelp.append("IPED Command-Line Tools Help\n");
        allHelp.append("============================\n\n");

        String[] cliSchemas = {
            "IPEDProcessingCLI",
            "IPEDWebAPICLI",
            "IPEDSearchAppCLI"
        };

        for (String schema : cliSchemas) {
            allHelp.append(generateHelp(schema)).append("\n");
            allHelp.append("---\n\n");
        }

        return allHelp.toString();
    }
}
