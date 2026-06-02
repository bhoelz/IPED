package iped.datasource;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataSourceInterfaceCompileCheckTest {

    @Test
    void iDataSource_isImplementable() {
        IDataSource ds = new IDataSource() {
            private String name;
            private String uuid;

            @Override public String getName() { return name; }
            @Override public File getSourceFile() { return null; }
            @Override public String getUUID() { return uuid; }
            @Override public void setName(String n) { name = n; }
            @Override public void setUUID(String u) { uuid = u; }
        };

        assertNull(ds.getName());
        ds.setName("Evidence-1");
        assertEquals("Evidence-1", ds.getName());
        ds.setUUID("abc-123");
        assertEquals("abc-123", ds.getUUID());
        assertNull(ds.getSourceFile());
    }
}
