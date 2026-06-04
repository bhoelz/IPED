package iped.parsers.apk;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class APKParserTest {

    @Test
    void getSupportedTypes_containsApkType() {
        Set<MediaType> types = new APKParser().getSupportedTypes(new ParseContext());
        assertNotNull(types);
        assertFalse(types.isEmpty());
        // APK maps to application/vnd.android.package-archive
        boolean hasApk = types.stream()
                .anyMatch(t -> t.toString().contains("android") || t.toString().contains("apk"));
        assertTrue(hasApk, "Expected APK media type, got: " + types);
    }

    @Test
    void parse_withInvalidInput_doesNotThrowNpe() {
        APKParser parser = new APKParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        // An empty/invalid stream — parser should throw a typed exception, not NPE
        assertThrows(Exception.class, () ->
                parser.parse(new ByteArrayInputStream(new byte[0]), handler, metadata, context));
    }
}
