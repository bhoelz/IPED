package iped.utils;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.*;

class XMLUtilTest {

    private static Document newDoc() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    @Test
    void getFirstElement_whenChildExists_thenReturnsIt() throws Exception {
        Document doc = newDoc();
        Element root = doc.createElement("root");
        doc.appendChild(root);
        Element child = doc.createElement("child");
        child.setTextContent("value");
        root.appendChild(child);

        Element result = XMLUtil.getFirstElement(root, "child");

        assertNotNull(result);
        assertEquals("value", result.getTextContent());
    }

    @Test
    void getFirstElement_whenMultipleChildren_thenReturnsFirst() throws Exception {
        Document doc = newDoc();
        Element root = doc.createElement("root");
        doc.appendChild(root);
        Element first = doc.createElement("item");
        first.setTextContent("first");
        root.appendChild(first);
        Element second = doc.createElement("item");
        second.setTextContent("second");
        root.appendChild(second);

        Element result = XMLUtil.getFirstElement(root, "item");

        assertNotNull(result);
        assertEquals("first", result.getTextContent());
    }

    @Test
    void getFirstElement_whenNoMatchingChild_thenNull() throws Exception {
        Document doc = newDoc();
        Element root = doc.createElement("root");
        doc.appendChild(root);

        Element result = XMLUtil.getFirstElement(root, "nonexistent");

        assertNull(result);
    }

    @Test
    void getFirstElement_whenNestedChild_thenFound() throws Exception {
        Document doc = newDoc();
        Element root = doc.createElement("root");
        doc.appendChild(root);
        Element parent = doc.createElement("parent");
        root.appendChild(parent);
        Element nested = doc.createElement("nested");
        nested.setTextContent("deep");
        parent.appendChild(nested);

        // getElementsByTagName searches the entire subtree
        Element result = XMLUtil.getFirstElement(root, "nested");

        assertNotNull(result);
        assertEquals("deep", result.getTextContent());
    }
}
