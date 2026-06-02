package iped.engine.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ItemId}.
 */
class ItemIdTest {

    @Test
    @DisplayName("Constructor should set sourceId and id")
    void constructor_shouldSetFields() {
        ItemId itemId = new ItemId(1, 42);
        assertEquals(1, itemId.getSourceId());
        assertEquals(42, itemId.getId());
    }

    @Test
    @DisplayName("compareTo with same sourceId should compare by id")
    void compareTo_sameSourceId_shouldCompareById() {
        ItemId a = new ItemId(1, 10);
        ItemId b = new ItemId(1, 20);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new ItemId(1, 10)));
    }

    @Test
    @DisplayName("compareTo with different sourceId should compare by sourceId")
    void compareTo_differentSourceId_shouldCompareBySourceId() {
        ItemId a = new ItemId(1, 100);
        ItemId b = new ItemId(2, 1);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    @DisplayName("compareTo with same sourceId and id should return 0")
    void compareTo_same_shouldReturnZero() {
        ItemId a = new ItemId(5, 10);
        ItemId b = new ItemId(5, 10);
        assertEquals(0, a.compareTo(b));
    }

    @Test
    @DisplayName("equals with same sourceId and id should return true")
    void equals_sameFields_shouldReturnTrue() {
        ItemId a = new ItemId(1, 42);
        ItemId b = new ItemId(1, 42);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
    }

    @Test
    @DisplayName("equals with different id should return false")
    void equals_differentId_shouldReturnFalse() {
        ItemId a = new ItemId(1, 42);
        ItemId b = new ItemId(1, 43);
        assertFalse(a.equals(b));
    }

    @Test
    @DisplayName("equals with different sourceId should return false")
    void equals_differentSourceId_shouldReturnFalse() {
        ItemId a = new ItemId(1, 42);
        ItemId b = new ItemId(2, 42);
        assertFalse(a.equals(b));
    }

    @Test
    @DisplayName("hashCode should return id")
    void hashCode_shouldReturnId() {
        ItemId itemId = new ItemId(3, 99);
        assertEquals(99, itemId.hashCode());
    }

    @Test
    @DisplayName("hashCode should be consistent for equal objects")
    void hashCode_equalObjects_shouldBeConsistent() {
        ItemId a = new ItemId(1, 42);
        ItemId b = new ItemId(1, 42);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("getSourceId should return sourceId")
    void getSourceId_shouldReturnSourceId() {
        ItemId itemId = new ItemId(7, 13);
        assertEquals(7, itemId.getSourceId());
    }

    @Test
    @DisplayName("getId should return id")
    void getId_shouldReturnId() {
        ItemId itemId = new ItemId(7, 13);
        assertEquals(13, itemId.getId());
    }

    @Test
    @DisplayName("compareTo with negative ids should work correctly")
    void compareTo_negativeIds_shouldWork() {
        ItemId a = new ItemId(0, -5);
        ItemId b = new ItemId(0, 5);
        assertTrue(a.compareTo(b) < 0);
    }

    @Test
    @DisplayName("compareTo with zero values should work")
    void compareTo_zeroValues_shouldWork() {
        ItemId a = new ItemId(0, 0);
        ItemId b = new ItemId(0, 0);
        assertEquals(0, a.compareTo(b));
    }
}
