package iped.parsers.compress;

import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline regression coverage added when this module was split out of
 * iped-parsers-impl with zero tests. Guards against each parser's
 * SUPPORTED_TYPES set going empty and the META-INF/services SPI wiring
 * breaking silently.
 */
class CompressionParsersTest {

    @Test
    void lzfseParser_supportedTypesNotEmpty() {
        Set<MediaType> types = new LZFSEParser().getSupportedTypes(new ParseContext());
        assertNotNull(types);
        assertFalse(types.isEmpty());
    }

    @Test
    void packageParser_supportedTypesNotEmpty() {
        Set<MediaType> types = new PackageParser().getSupportedTypes(new ParseContext());
        assertNotNull(types);
        assertFalse(types.isEmpty());
    }

    @Test
    void rarParser_supportedTypesNotEmpty() {
        Set<MediaType> types = new RARParser().getSupportedTypes(new ParseContext());
        assertNotNull(types);
        assertFalse(types.isEmpty());
    }

    @Test
    void sevenZipParser_supportedTypesNotEmpty() {
        Set<MediaType> types = new SevenZipParser().getSupportedTypes(new ParseContext());
        assertNotNull(types);
        assertFalse(types.isEmpty());
    }

    @Test
    void allFourParsersRegisteredViaServiceLoader() {
        boolean lzfse = false, pkg = false, rar = false, sevenZip = false;
        for (Parser p : ServiceLoader.load(Parser.class, CompressionParsersTest.class.getClassLoader())) {
            if (p instanceof LZFSEParser) lzfse = true;
            if (p instanceof PackageParser) pkg = true;
            if (p instanceof RARParser) rar = true;
            if (p instanceof SevenZipParser) sevenZip = true;
        }
        assertTrue(lzfse, "LZFSEParser must be discoverable via META-INF/services");
        assertTrue(pkg, "PackageParser must be discoverable via META-INF/services");
        assertTrue(rar, "RARParser must be discoverable via META-INF/services");
        assertTrue(sevenZip, "SevenZipParser must be discoverable via META-INF/services");
    }
}
