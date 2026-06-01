package iped.engine.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for {@link DataSource}.
 */
class DataSourceTest {

    @Test
    @DisplayName("Default constructor should create instance with null fields")
    void defaultConstructor_shouldCreateWithNullFields() {
        DataSource ds = new DataSource();
        assertNull(ds.getSourceFile());
        assertNull(ds.getName());
        assertThrows(NullPointerException.class, ds::getUUID);
    }

    @Test
    @DisplayName("Constructor with source should set sourceFile and generate UUID")
    void constructorWithSource_shouldSetSourceAndUUID() {
        File source = new File("/path/to/evidence.img");
        DataSource ds = new DataSource(source);
        assertEquals(source, ds.getSourceFile());
        assertNotNull(ds.getUUID());
        assertDoesNotThrow(() -> UUID.fromString(ds.getUUID()));
    }

    @Test
    @DisplayName("getUUID should return valid UUID string")
    void getUUID_shouldReturnValidUUID() {
        DataSource ds = new DataSource(new File("test.img"));
        String uuid = ds.getUUID();
        assertNotNull(uuid);
        assertDoesNotThrow(() -> UUID.fromString(uuid));
    }

    @Test
    @DisplayName("setUUID should update the UUID")
    void setUUID_shouldUpdateUUID() {
        DataSource ds = new DataSource(new File("test.img"));
        String originalUUID = ds.getUUID();

        String newUUID = "550e8400-e29b-41d4-a716-446655440000";
        ds.setUUID(newUUID);
        assertEquals(newUUID, ds.getUUID());
        assertNotEquals(originalUUID, ds.getUUID());
    }

    @Test
    @DisplayName("setUUID with invalid UUID should throw")
    void setUUID_invalidUUID_shouldThrow() {
        DataSource ds = new DataSource(new File("test.img"));
        assertThrows(IllegalArgumentException.class, () -> ds.setUUID("not-a-uuid"));
    }

    @Test
    @DisplayName("setName and getName should work correctly")
    void setAndGetName_shouldWork() {
        DataSource ds = new DataSource(new File("test.img"));
        assertNull(ds.getName());
        ds.setName("My Evidence");
        assertEquals("My Evidence", ds.getName());
    }

    @Test
    @DisplayName("getSourceFile should return the file passed to constructor")
    void getSourceFile_shouldReturnConstructorFile() {
        File source = new File("/evidence/test.E01");
        DataSource ds = new DataSource(source);
        assertEquals(source, ds.getSourceFile());
    }

    @Test
    @DisplayName("toString should return UUID")
    void toString_shouldReturnUUID() {
        DataSource ds = new DataSource(new File("test.img"));
        assertEquals(ds.getUUID(), ds.toString());
    }

    @Test
    @DisplayName("Two DataSource instances should have different UUIDs")
    void twoInstances_shouldHaveDifferentUUIDs() {
        DataSource ds1 = new DataSource(new File("a.img"));
        DataSource ds2 = new DataSource(new File("b.img"));
        assertNotEquals(ds1.getUUID(), ds2.getUUID());
    }
}