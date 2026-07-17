package iped.engine.webapi.json;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DocPropsJSONTest {

  @Test
  void settersAndGetters_roundTrip() {
    DocPropsJSON doc = new DocPropsJSON();
    doc.setSource("source-A");
    doc.setId(42);
    doc.setLuceneId(100);
    doc.setProperties(Map.of("title", new String[] {"My Doc"}, "author", new String[] {"Alice"}));
    doc.setBookmarks(List.of("bookmark1", "bookmark2"));
    doc.setSelected(true);

    assertEquals("source-A", doc.getSource());
    assertEquals(42, doc.getId());
    assertEquals(100, doc.getLuceneId());
    assertArrayEquals(new String[] {"My Doc"}, doc.getProperties().get("title"));
    assertEquals(List.of("bookmark1", "bookmark2"), doc.getBookmarks());
    assertTrue(doc.isSelected());
  }

  @Test
  void defaults_whenNew_thenNullAndFalse() {
    DocPropsJSON doc = new DocPropsJSON();
    assertNull(doc.getSource());
    assertEquals(0, doc.getId());
    assertEquals(0, doc.getLuceneId());
    assertNull(doc.getProperties());
    assertNull(doc.getBookmarks());
    assertFalse(doc.isSelected());
  }

  @Test
  void setSelected_whenTrue_thenIsSelectedTrue() {
    DocPropsJSON doc = new DocPropsJSON();
    doc.setSelected(true);
    assertTrue(doc.isSelected());
    doc.setSelected(false);
    assertFalse(doc.isSelected());
  }
}
