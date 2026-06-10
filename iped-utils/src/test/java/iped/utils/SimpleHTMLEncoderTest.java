package iped.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SimpleHTMLEncoderTest {

    @Test
    void htmlEncode_whenNull_thenEmpty() {
        assertEquals("", SimpleHTMLEncoder.htmlEncode(null));
    }

    @Test
    void htmlEncode_whenEmpty_thenEmpty() {
        assertEquals("", SimpleHTMLEncoder.htmlEncode(""));
    }

    @Test
    void htmlEncode_whenLessThan_thenEncoded() {
        assertEquals("&lt;", SimpleHTMLEncoder.htmlEncode("<"));
    }

    @Test
    void htmlEncode_whenGreaterThan_thenEncoded() {
        assertEquals("&gt;", SimpleHTMLEncoder.htmlEncode(">"));
    }

    @Test
    void htmlEncode_whenAmpersand_thenEncoded() {
        assertEquals("&amp;", SimpleHTMLEncoder.htmlEncode("&"));
    }

    @Test
    void htmlEncode_whenDoubleQuote_thenEncoded() {
        assertEquals("&quot;", SimpleHTMLEncoder.htmlEncode("\""));
    }

    @Test
    void htmlEncode_whenSingleQuote_thenEncoded() {
        assertEquals("&#39;", SimpleHTMLEncoder.htmlEncode("'"));
    }

    @Test
    void htmlEncode_whenMixedHtml_thenAllEncoded() {
        String input = "<script>alert('xss & \"attack\"')</script>";
        String result = SimpleHTMLEncoder.htmlEncode(input);
        assertFalse(result.contains("<"));
        assertFalse(result.contains(">"));
        assertFalse(result.contains("'"));
        assertFalse(result.contains("\""));
        assertFalse(result.contains("&script"));
    }

    @Test
    void htmlEncode_whenPlainText_thenUnchanged() {
        assertEquals("hello world 123", SimpleHTMLEncoder.htmlEncode("hello world 123"));
    }

    @Test
    void htmlEncode_unicodePassthrough() {
        String unicode = "café";
        assertEquals(unicode, SimpleHTMLEncoder.htmlEncode(unicode));
    }

    @Test
    void htmlDecode_whenNull_thenEmpty() {
        assertEquals("", SimpleHTMLEncoder.htmlDecode(null));
    }

    @Test
    void htmlDecode_roundTrip() {
        String original = "<div class=\"test\">&amp; 'hello'</div>";
        String encoded = SimpleHTMLEncoder.htmlEncode(original);
        String decoded = SimpleHTMLEncoder.htmlDecode(encoded);
        assertEquals(original, decoded);
    }

    @Test
    void encodeText_instanceMethod_sameAsStatic() {
        SimpleHTMLEncoder encoder = new SimpleHTMLEncoder();
        String input = "<b>bold</b>";
        assertEquals(SimpleHTMLEncoder.htmlEncode(input), encoder.encodeText(input));
    }
}
