package iped.parsers.threema;

import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline regression coverage added when this module was split out of
 * iped-parsers-impl with zero tests. Guards against the SUPPORTED_TYPES set
 * going empty and the META-INF/services SPI wiring breaking silently.
 */
class ThreemaParserTest {

    @Test
    void getSupportedTypes_isNotEmpty() {
        Set<MediaType> types = new ThreemaParser().getSupportedTypes(new ParseContext());
        assertNotNull(types);
        assertFalse(types.isEmpty());
    }

    @Test
    void registeredViaServiceLoader() {
        boolean found = false;
        for (org.apache.tika.parser.Parser p : ServiceLoader.load(org.apache.tika.parser.Parser.class,
                ThreemaParserTest.class.getClassLoader())) {
            if (p instanceof ThreemaParser) {
                found = true;
                break;
            }
        }
        assertTrue(found, "ThreemaParser must be discoverable via META-INF/services");
    }
}
