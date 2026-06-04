package iped.parsers.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedItemTest {

    // --- EmbeddedParent ---

    @Test
    void embeddedParent_constructor_storesObject() {
        Object payload = "test-object";
        EmbeddedParent parent = new EmbeddedParent(payload);
        assertSame(payload, parent.getObj());
    }

    @Test
    void embeddedParent_setObj_updatesObject() {
        EmbeddedParent parent = new EmbeddedParent("original");
        Object replacement = Integer.valueOf(42);
        parent.setObj(replacement);
        assertSame(replacement, parent.getObj());
    }

    @Test
    void embeddedParent_withNull_thenGetObjNull() {
        EmbeddedParent parent = new EmbeddedParent(null);
        assertNull(parent.getObj());
    }

    // --- EmbeddedItem (extends EmbeddedParent) ---

    @Test
    void embeddedItem_constructor_storesObject() {
        Object payload = "item-data";
        EmbeddedItem item = new EmbeddedItem(payload);
        assertSame(payload, item.getObj());
    }

    @Test
    void embeddedItem_isDeprecated() throws Exception {
        assertTrue(EmbeddedItem.class.isAnnotationPresent(Deprecated.class));
    }

    @Test
    void embeddedParent_isDeprecated() throws Exception {
        assertTrue(EmbeddedParent.class.isAnnotationPresent(Deprecated.class));
    }

    @Test
    void embeddedItem_extendsEmbeddedParent() {
        EmbeddedItem item = new EmbeddedItem("x");
        assertInstanceOf(EmbeddedParent.class, item);
    }
}
