package iped.engine.webapi.json;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocIDJSONTest {

    // --- DocIDJSON ---

    @Test
    void docIDJSON_defaultConstructor_thenNullAndZero() {
        DocIDJSON doc = new DocIDJSON();
        assertNull(doc.getSource());
        assertEquals(0, doc.getId());
    }

    @Test
    void docIDJSON_paramConstructor_thenFieldsSet() {
        DocIDJSON doc = new DocIDJSON("source-X", 99);
        assertEquals("source-X", doc.getSource());
        assertEquals(99, doc.getId());
    }

    @Test
    void docIDJSON_setters_roundTrip() {
        DocIDJSON doc = new DocIDJSON();
        doc.setSource("src");
        doc.setId(7);
        assertEquals("src", doc.getSource());
        assertEquals(7, doc.getId());
    }

    // --- DocIDGroupJSON ---

    @Test
    void docIDGroupJSON_defaultConstructor_thenNullFields() {
        DocIDGroupJSON grp = new DocIDGroupJSON();
        assertNull(grp.getSource());
        assertNull(grp.getIds());
    }

    @Test
    void docIDGroupJSON_paramConstructor_thenFieldsSet() {
        List<Integer> ids = List.of(1, 2, 3);
        DocIDGroupJSON grp = new DocIDGroupJSON("src-A", ids);
        assertEquals("src-A", grp.getSource());
        assertEquals(ids, grp.getIds());
    }

    @Test
    void docIDGroupJSON_setters_roundTrip() {
        DocIDGroupJSON grp = new DocIDGroupJSON();
        grp.setSource("src-B");
        grp.setIds(List.of(10, 20));
        assertEquals("src-B", grp.getSource());
        assertEquals(List.of(10, 20), grp.getIds());
    }
}
