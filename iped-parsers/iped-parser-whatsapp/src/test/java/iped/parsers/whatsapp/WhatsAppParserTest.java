package iped.parsers.whatsapp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ServiceLoader;
import java.util.Set;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.junit.jupiter.api.Test;

/**
 * Baseline regression coverage added when this module was split out of iped-parsers-impl with zero
 * tests. Guards against the SUPPORTED_TYPES set going empty and the META-INF/services SPI wiring
 * breaking silently.
 */
class WhatsAppParserTest {

  @Test
  void getSupportedTypes_isNotEmpty() {
    Set<MediaType> types = new WhatsAppParser().getSupportedTypes(new ParseContext());
    assertNotNull(types);
    assertFalse(types.isEmpty());
  }

  @Test
  void registeredViaServiceLoader() {
    boolean found = false;
    for (org.apache.tika.parser.Parser p :
        ServiceLoader.load(
            org.apache.tika.parser.Parser.class, WhatsAppParserTest.class.getClassLoader())) {
      if (p instanceof WhatsAppParser) {
        found = true;
        break;
      }
    }
    assertTrue(found, "WhatsAppParser must be discoverable via META-INF/services");
  }
}
