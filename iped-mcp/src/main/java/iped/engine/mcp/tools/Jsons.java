package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

final class Jsons {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Jsons() {
    }

    static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize value to JSON", e);
        }
    }
}
