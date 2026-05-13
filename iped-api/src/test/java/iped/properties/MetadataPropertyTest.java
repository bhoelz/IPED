package iped.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MetadataPropertyTest {

    @Test
    void internalDate_whenCalled_thenKeepsName() {
        MetadataProperty property = MetadataProperty.internalDate("created");

        assertEquals("created", property.getName());
    }

    @Test
    void internalBooleanAndInternalInteger_whenCalled_thenKeepNames() {
        MetadataProperty bool = MetadataProperty.internalBoolean("enabled");
        MetadataProperty integer = MetadataProperty.internalInteger("count");

        assertEquals("enabled", bool.getName());
        assertEquals("count", integer.getName());
    }
}

