package iped.parsers.vcard;

import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VCardParser that do not require charset detection services.
 * Full parse integration tests exist in iped-parsers-impl (which has the full
 * Tika stack on the classpath).
 */
class VCardParserTest {

    @Test
    void getSupportedTypes_containsVcardMediaType() {
        Set<MediaType> types = new VCardParser().getSupportedTypes(new ParseContext());
        assertNotNull(types);
        assertFalse(types.isEmpty());
        boolean hasVcard = types.stream()
                .anyMatch(t -> t.toString().contains("vcard") || t.toString().contains("x-vcard"));
        assertTrue(hasVcard, "Expected vcard media type, got: " + types);
    }

    @Test
    void getSupportedTypes_returnsConsistentResult() {
        VCardParser parser = new VCardParser();
        ParseContext ctx = new ParseContext();
        Set<MediaType> first = parser.getSupportedTypes(ctx);
        Set<MediaType> second = parser.getSupportedTypes(ctx);
        assertEquals(first, second, "getSupportedTypes must be deterministic");
    }

    @Test
    void constructor_canBeInstantiated() {
        assertNotNull(new VCardParser());
    }
}
