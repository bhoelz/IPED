package iped.parsers.ufed;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ServiceLoader;
import java.util.Set;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.junit.jupiter.api.Test;

/**
 * Baseline regression coverage added when this module was split out of iped-parsers-impl with zero
 * tests. Guards against each parser's SUPPORTED_TYPES set going empty and the META-INF/services SPI
 * wiring breaking silently.
 */
class UfedParsersTest {

  @Test
  void ufedAccountableParser_supportedTypesNotEmpty() {
    Set<MediaType> types = new UfedAccountableParser().getSupportedTypes(new ParseContext());
    assertNotNull(types);
    assertFalse(types.isEmpty());
  }

  @Test
  void ufedChatParser_supportedTypesNotEmpty() {
    Set<MediaType> types = new UfedChatParser().getSupportedTypes(new ParseContext());
    assertNotNull(types);
    assertFalse(types.isEmpty());
  }

  @Test
  void ufedEmailParser_supportedTypesNotEmpty() {
    Set<MediaType> types = new UfedEmailParser().getSupportedTypes(new ParseContext());
    assertNotNull(types);
    assertFalse(types.isEmpty());
  }

  @Test
  void ufedMessageParser_supportedTypesNotEmpty() {
    Set<MediaType> types = new UfedMessageParser().getSupportedTypes(new ParseContext());
    assertNotNull(types);
    assertFalse(types.isEmpty());
  }

  @Test
  void allFourParsersRegisteredViaServiceLoader() {
    boolean accountable = false, chat = false, email = false, message = false;
    for (Parser p : ServiceLoader.load(Parser.class, UfedParsersTest.class.getClassLoader())) {
      if (p instanceof UfedAccountableParser) accountable = true;
      if (p instanceof UfedChatParser) chat = true;
      if (p instanceof UfedEmailParser) email = true;
      if (p instanceof UfedMessageParser) message = true;
    }
    assertTrue(accountable, "UfedAccountableParser must be discoverable via META-INF/services");
    assertTrue(chat, "UfedChatParser must be discoverable via META-INF/services");
    assertTrue(email, "UfedEmailParser must be discoverable via META-INF/services");
    assertTrue(message, "UfedMessageParser must be discoverable via META-INF/services");
  }
}
