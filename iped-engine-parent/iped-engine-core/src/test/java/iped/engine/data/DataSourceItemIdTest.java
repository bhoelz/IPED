package iped.engine.data;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceItemIdTest {

    @Test
    void dataSourceShouldExposeFileNameAndUuid() {
        DataSource dataSource = new DataSource(new File("x.ad1"));
        assertEquals(new File("x.ad1"), dataSource.getSourceFile());
        assertNotNull(dataSource.getUUID());
        assertEquals(dataSource.getUUID(), dataSource.toString());
    }

    @Test
    void dataSourceShouldAcceptKnownUuid() {
        DataSource dataSource = new DataSource(new File("x.ad1"));
        String uuid = UUID.randomUUID().toString();
        dataSource.setUUID(uuid);
        dataSource.setName("name");
        assertEquals(uuid, dataSource.getUUID());
        assertEquals("name", dataSource.getName());
    }

    @Test
    void defaultDataSourceShouldFailWhenUuidNotSet() {
        DataSource dataSource = new DataSource();
        assertThrows(NullPointerException.class, dataSource::getUUID);
    }

    @Test
    void itemIdCompareEqualsAndHashCodeShouldMatchCurrentImplementation() {
        ItemId a = new ItemId(1, 10);
        ItemId b = new ItemId(1, 11);
        ItemId c = new ItemId(2, 1);
        ItemId same = new ItemId(1, 10);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(c.compareTo(a) > 0);
        assertEquals(a, same);
        assertNotEquals(a, b);
        assertEquals(10, a.hashCode());
    }

    @Test
    void itemIdEqualsWithWrongTypeShouldThrowAsImplemented() {
        ItemId a = new ItemId(1, 10);
        assertThrows(ClassCastException.class, () -> a.equals("x"));
    }
}

