package iped.parsers.util;

import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IgnoreContentHandlerTest {

    @Test
    void constructor_thenNotNull() {
        assertNotNull(new IgnoreContentHandler());
    }

    @Test
    void startDocument_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().startDocument());
    }

    @Test
    void endDocument_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().endDocument());
    }

    @Test
    void startElement_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().startElement("", "tag", "tag", new AttributesImpl()));
    }

    @Test
    void endElement_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().endElement("", "tag", "tag"));
    }

    @Test
    void characters_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().characters("hello".toCharArray(), 0, 5));
    }

    @Test
    void ignorableWhitespace_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().ignorableWhitespace(" ".toCharArray(), 0, 1));
    }

    @Test
    void processingInstruction_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().processingInstruction("target", "data"));
    }

    @Test
    void skippedEntity_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().skippedEntity("entity"));
    }

    @Test
    void startPrefixMapping_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().startPrefixMapping("ns", "http://example.com"));
    }

    @Test
    void endPrefixMapping_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().endPrefixMapping("ns"));
    }

    @Test
    void setDocumentLocator_noException() {
        assertDoesNotThrow(() -> new IgnoreContentHandler().setDocumentLocator(null));
    }

    @Test
    void allEventsInSequence_noException() {
        assertDoesNotThrow(() -> {
            IgnoreContentHandler h = new IgnoreContentHandler();
            h.startDocument();
            h.startPrefixMapping("", "");
            h.startElement("", "root", "root", new AttributesImpl());
            h.characters("text".toCharArray(), 0, 4);
            h.endElement("", "root", "root");
            h.endPrefixMapping("");
            h.endDocument();
        });
    }
}
