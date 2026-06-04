package iped.parsers.util;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.jupiter.api.Assertions.*;

class ToXMLContentHandlerTest {

    @Test
    void noArgConstructor_thenNotNull() {
        assertNotNull(new ToXMLContentHandler());
    }

    @Test
    void startDocument_withEncoding_writesXmlProlog() throws SAXException {
        // The (String encoding) constructor uses an internal StringWriter
        ToXMLContentHandler handler = new ToXMLContentHandler("UTF-8");
        handler.startDocument();
        String output = handler.toString();
        assertTrue(output.contains("<?xml"), "Expected XML prolog, got: " + output);
        assertTrue(output.contains("UTF-8"), "Expected encoding in prolog");
    }

    @Test
    void startDocument_withNullEncoding_noProlog() throws SAXException {
        // No-arg constructor → encoding is null → startDocument writes nothing
        ToXMLContentHandler handler = new ToXMLContentHandler();
        handler.startDocument();
        assertEquals("", handler.toString());
    }

    @Test
    void startElement_writesOpenTag() throws SAXException {
        ToXMLContentHandler handler = new ToXMLContentHandler();
        handler.startElement("", "root", "root", new AttributesImpl());
        // lazyCloseStartElement is triggered by the next call; flush it with endElement
        handler.endElement("", "root", "root");
        String output = handler.toString();
        assertTrue(output.contains("<root"), "Expected <root in: " + output);
    }

    @Test
    void endElement_writesCloseTagOrSelfClose() throws SAXException {
        ToXMLContentHandler handler = new ToXMLContentHandler();
        handler.startElement("", "p", "p", new AttributesImpl());
        handler.endElement("", "p", "p");
        String output = handler.toString();
        // Empty element may be serialized as <p /> or <p></p>
        assertTrue(output.contains("p"), "Expected element name in: " + output);
        assertTrue(output.contains("<p"), "Expected opening <p in: " + output);
    }

    @Test
    void characters_writesTextContent() throws SAXException {
        ToXMLContentHandler handler = new ToXMLContentHandler();
        handler.startElement("", "span", "span", new AttributesImpl());
        char[] text = "hello world".toCharArray();
        handler.characters(text, 0, text.length);
        handler.endElement("", "span", "span");
        assertTrue(handler.toString().contains("hello world"));
    }

    @Test
    void characters_escapesLessThan() throws SAXException {
        ToXMLContentHandler handler = new ToXMLContentHandler();
        handler.startElement("", "span", "span", new AttributesImpl());
        char[] text = "<script>".toCharArray();
        handler.characters(text, 0, text.length);
        handler.endElement("", "span", "span");
        String output = handler.toString();
        assertFalse(output.contains("<script>"), "Raw < should be escaped");
        assertTrue(output.contains("&lt;"), "Expected &lt; in: " + output);
    }

    @Test
    void characters_escapesAmpersand() throws SAXException {
        ToXMLContentHandler handler = new ToXMLContentHandler();
        handler.startElement("", "span", "span", new AttributesImpl());
        char[] text = "a & b".toCharArray();
        handler.characters(text, 0, text.length);
        handler.endElement("", "span", "span");
        assertTrue(handler.toString().contains("&amp;"));
    }

    @Test
    void startElement_withAttribute_writesAttribute() throws SAXException {
        ToXMLContentHandler handler = new ToXMLContentHandler();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "id", "id", "CDATA", "42");
        handler.startElement("", "div", "div", attrs);
        handler.endElement("", "div", "div");
        String output = handler.toString();
        assertTrue(output.contains("id=\"42\""), "Expected attribute in: " + output);
    }

    @Test
    void fullRoundTrip_producesWellFormedXml() throws SAXException {
        ToXMLContentHandler handler = new ToXMLContentHandler("UTF-8");
        handler.startDocument();
        handler.startElement("", "root", "root", new AttributesImpl());
        handler.startElement("", "item", "item", new AttributesImpl());
        char[] content = "data".toCharArray();
        handler.characters(content, 0, content.length);
        handler.endElement("", "item", "item");
        handler.endElement("", "root", "root");
        String output = handler.toString();
        assertTrue(output.contains("<root"));
        assertTrue(output.contains("<item>"));
        assertTrue(output.contains("data"));
        assertTrue(output.contains("</item>"));
        assertTrue(output.contains("</root>"));
    }
}
