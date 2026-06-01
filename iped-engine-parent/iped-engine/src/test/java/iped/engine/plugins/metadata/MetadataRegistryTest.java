package iped.engine.plugins.metadata;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for MetadataRegistry.
 */
public class MetadataRegistryTest {

    private MetadataRegistry registry;

    @Before
    public void setUp() {
        registry = new MetadataRegistry();
    }

    @Test
    public void testRegisterProperty() {
        MetadataPropertyDescriptor prop = new MetadataPropertyDescriptor(
            "test.property",
            "Test Property",
            "string",
            true,
            false,
            "standard",
            "test.category",
            "test-component"
        );

        registry.registerProperty(prop);
        assertNotNull(registry.getProperty("test.property"));
    }

    @Test
    public void testRegisterMultipleProperties() {
        registry.registerProperty(new MetadataPropertyDescriptor(
            "prop1", "Property 1", "string", true, false, "standard", "cat1", "comp1"
        ));
        registry.registerProperty(new MetadataPropertyDescriptor(
            "prop2", "Property 2", "integer", true, true, "standard", "cat2", "comp1"
        ));

        assertEquals(2, registry.getAllProperties().size());
    }

    @Test
    public void testGetProperty() {
        MetadataPropertyDescriptor prop = new MetadataPropertyDescriptor(
            "test.name",
            "Test Name",
            "string",
            true,
            false,
            "standard",
            "test",
            "component"
        );

        registry.registerProperty(prop);
        MetadataPropertyDescriptor retrieved = registry.getProperty("test.name");

        assertNotNull(retrieved);
        assertEquals("test.name", retrieved.name());
        assertEquals("Test Name", retrieved.displayName());
    }

    @Test
    public void testDuplicatePropertyRegistration() {
        MetadataPropertyDescriptor prop1 = new MetadataPropertyDescriptor(
            "dup.property",
            "Display 1",
            "string",
            true,
            false,
            "standard",
            "cat1",
            "comp1"
        );
        MetadataPropertyDescriptor prop2 = new MetadataPropertyDescriptor(
            "dup.property",
            "Display 2",
            "integer",
            false,
            true,
            "keyword",
            "cat2",
            "comp2"
        );

        registry.registerProperty(prop1);
        registry.registerProperty(prop2);

        // Last registration wins
        assertEquals("Display 2", registry.getProperty("dup.property").displayName());
    }

    @Test
    public void testGetAllProperties() {
        registry.registerProperty(new MetadataPropertyDescriptor(
            "prop1", "P1", "string", true, false, "standard", "cat", "comp"
        ));
        registry.registerProperty(new MetadataPropertyDescriptor(
            "prop2", "P2", "integer", false, true, "standard", "cat", "comp"
        ));

        List<MetadataPropertyDescriptor> all = registry.getAllProperties();
        assertEquals(2, all.size());
    }

    @Test
    public void testGetIndexedProperties() {
        registry.registerProperty(new MetadataPropertyDescriptor(
            "indexed1", "I1", "string", true, false, "standard", "cat", "comp"
        ));
        registry.registerProperty(new MetadataPropertyDescriptor(
            "not_indexed", "N", "string", false, false, "standard", "cat", "comp"
        ));
        registry.registerProperty(new MetadataPropertyDescriptor(
            "indexed2", "I2", "integer", true, true, "standard", "cat", "comp"
        ));

        List<MetadataPropertyDescriptor> indexed = registry.getIndexedProperties();
        assertEquals(2, indexed.size());

        boolean hasIndexed1 = indexed.stream().anyMatch(p -> p.name().equals("indexed1"));
        boolean hasIndexed2 = indexed.stream().anyMatch(p -> p.name().equals("indexed2"));
        assertTrue(hasIndexed1);
        assertTrue(hasIndexed2);
    }

    @Test
    public void testGetPropertiesByComponent() {
        registry.registerProperty(new MetadataPropertyDescriptor(
            "comp1.prop1", "C1P1", "string", true, false, "standard", "cat", "comp1"
        ));
        registry.registerProperty(new MetadataPropertyDescriptor(
            "comp1.prop2", "C1P2", "integer", true, false, "standard", "cat", "comp1"
        ));
        registry.registerProperty(new MetadataPropertyDescriptor(
            "comp2.prop1", "C2P1", "string", true, false, "standard", "cat", "comp2"
        ));

        List<MetadataPropertyDescriptor> comp1Props = registry.getPropertiesByComponent("comp1");
        assertEquals(2, comp1Props.size());

        List<MetadataPropertyDescriptor> comp2Props = registry.getPropertiesByComponent("comp2");
        assertEquals(1, comp2Props.size());
    }

    @Test
    public void testPropertyTypeValidation() {
        MetadataPropertyDescriptor prop = new MetadataPropertyDescriptor(
            "type.test",
            "Type Test",
            "string",
            true,
            false,
            "standard",
            "cat",
            "comp"
        );

        registry.registerProperty(prop);
        assertEquals("string", registry.getProperty("type.test").dataType());
    }

    @Test
    public void testDataTypes() {
        String[] dataTypes = {"date", "boolean", "integer", "string", "decimal"};

        for (int i = 0; i < dataTypes.length; i++) {
            MetadataPropertyDescriptor prop = new MetadataPropertyDescriptor(
                "type." + i,
                "Type " + i,
                dataTypes[i],
                true,
                false,
                "standard",
                "cat",
                "comp"
            );
            registry.registerProperty(prop);
        }

        assertEquals(5, registry.getAllProperties().size());
    }

    @Test
    public void testGetStatistics() {
        registry.registerProperty(new MetadataPropertyDescriptor(
            "prop1", "P1", "string", true, false, "standard", "cat", "comp1"
        ));
        registry.registerProperty(new MetadataPropertyDescriptor(
            "prop2", "P2", "integer", true, false, "standard", "cat", "comp1"
        ));

        Map<String, Object> stats = registry.getStatistics();
        assertNotNull(stats);
        assertEquals(2, stats.get("total_properties"));
        assertEquals(2, stats.get("indexed_properties"));
    }

    @Test
    public void testPropertyComparison() {
        MetadataPropertyDescriptor prop1 = new MetadataPropertyDescriptor(
            "same.name",
            "Display 1",
            "string",
            true,
            false,
            "standard",
            "cat1",
            "comp1"
        );
        MetadataPropertyDescriptor prop2 = new MetadataPropertyDescriptor(
            "same.name",
            "Display 2",
            "integer",
            false,
            true,
            "keyword",
            "cat2",
            "comp2"
        );

        // Properties with same name should be equal
        assertEquals(prop1, prop2);
    }
}
