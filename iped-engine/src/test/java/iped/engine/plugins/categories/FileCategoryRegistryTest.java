package iped.engine.plugins.categories;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for FileCategoryRegistry.
 */
public class FileCategoryRegistryTest {

    private FileCategoryRegistry registry;

    @Before
    public void setUp() {
        registry = new FileCategoryRegistry();
    }

    @Test
    public void testRegisterCategory() {
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp1");

        assertNotNull(registry.getCategory("Forensic/Windows/Registry"));
    }

    @Test
    public void testRegisterMultipleCategories() {
        registry.registerCategory("Forensic/Windows", "Windows", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Linux", "Linux", "icon", "desc", "comp2");
        registry.registerCategory("Evidence", "Evidence", "icon", "desc", "comp1");

        assertEquals(3, registry.getAllCategories().size());
    }

    @Test
    public void testCategoryHierarchyCreation() {
        registry.registerCategory("Forensic/Windows/Registry/HKLM", "HKLM", "icon", "desc", "comp1");

        // Verify all parent categories were created
        assertNotNull(registry.getCategory("Forensic"));
        assertNotNull(registry.getCategory("Forensic/Windows"));
        assertNotNull(registry.getCategory("Forensic/Windows/Registry"));
        assertNotNull(registry.getCategory("Forensic/Windows/Registry/HKLM"));
    }

    @Test
    public void testGetSubcategories() {
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Windows/EventLogs", "Event Logs", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Linux/Logs", "Logs", "icon", "desc", "comp2");

        List<String> subcats = registry.getSubcategories("Forensic/Windows");
        assertEquals(2, subcats.size());
        assertTrue(subcats.contains("Forensic/Windows/Registry"));
        assertTrue(subcats.contains("Forensic/Windows/EventLogs"));
    }

    @Test
    public void testGetRootCategories() {
        registry.registerCategory("Forensic/Windows", "Windows", "icon", "desc", "comp1");
        registry.registerCategory("Evidence/Physical", "Physical", "icon", "desc", "comp2");
        registry.registerCategory("Forensic/Linux", "Linux", "icon", "desc", "comp1");

        List<String> roots = registry.getRootCategories();
        assertEquals(2, roots.size());
        assertTrue(roots.contains("Forensic"));
        assertTrue(roots.contains("Evidence"));
    }

    @Test
    public void testGetLeafCategories() {
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Windows/Registry/HKLM", "HKLM", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Linux", "Linux", "icon", "desc", "comp2");

        List<String> leaves = registry.getLeafCategories();
        assertEquals(2, leaves.size());
        assertTrue(leaves.contains("Forensic/Windows/Registry/HKLM"));
        assertTrue(leaves.contains("Forensic/Linux"));
    }

    @Test
    public void testGetCategoryDepth() {
        registry.registerCategory("A", "A", "icon", "desc", "comp");
        registry.registerCategory("A/B", "B", "icon", "desc", "comp");
        registry.registerCategory("A/B/C", "C", "icon", "desc", "comp");
        registry.registerCategory("A/B/C/D", "D", "icon", "desc", "comp");

        assertEquals(1, registry.getCategoryDepth("A"));
        assertEquals(2, registry.getCategoryDepth("A/B"));
        assertEquals(3, registry.getCategoryDepth("A/B/C"));
        assertEquals(4, registry.getCategoryDepth("A/B/C/D"));
    }

    @Test
    public void testGetCategoriesByDepth() {
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Windows/EventLogs", "Event Logs", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Linux", "Linux", "icon", "desc", "comp2");
        registry.registerCategory("Evidence", "Evidence", "icon", "desc", "comp1");

        List<String> depth1 = registry.getCategoriesByDepth(1);
        assertEquals(2, depth1.size()); // Forensic, Evidence

        List<String> depth2 = registry.getCategoriesByDepth(2);
        assertEquals(2, depth2.size()); // Forensic/Windows, Forensic/Linux
    }

    @Test
    public void testGetPropertiesByComponent() {
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Windows/EventLogs", "Event Logs", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Linux", "Linux", "icon", "desc", "comp2");

        List<String> comp1Cats = registry.getCategoriesByComponent("comp1");
        assertEquals(3, comp1Cats.size()); // Registry, EventLogs, and parents

        List<String> comp2Cats = registry.getCategoriesByComponent("comp2");
        assertEquals(2, comp2Cats.size()); // Linux and Forensic
    }

    @Test
    public void testCategoryHierarchyTraversal() {
        registry.registerCategory("A/B/C", "C", "icon", "desc", "comp");

        List<String> path = registry.getPath("A/B/C");
        assertEquals(3, path.size());
        assertEquals("A", path.get(0));
        assertEquals("A/B", path.get(1));
        assertEquals("A/B/C", path.get(2));
    }

    @Test
    public void testGetCategoryInfo() {
        registry.registerCategory("Forensic/Windows/Registry", "Registry Hive", "reg-icon", "Registry hive files", "comp1");

        FileCategoryRegistry.CategoryDescriptor desc = registry.getCategoryInfo("Forensic/Windows/Registry");
        assertNotNull(desc);
        assertEquals("Registry Hive", desc.displayName());
        assertEquals("reg-icon", desc.icon());
        assertEquals("Registry hive files", desc.description());
    }

    @Test
    public void testParentCategoryAutocreation() {
        // Registering deep category should auto-create parents
        registry.registerCategory("Root/Level1/Level2/Level3", "L3", "icon", "desc", "comp");

        // All parents should exist with default display names
        assertNotNull(registry.getCategory("Root"));
        assertNotNull(registry.getCategory("Root/Level1"));
        assertNotNull(registry.getCategory("Root/Level1/Level2"));
        assertNotNull(registry.getCategory("Root/Level1/Level2/Level3"));
    }

    @Test
    public void testGetStatistics() {
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Windows/EventLogs", "Event Logs", "icon", "desc", "comp1");
        registry.registerCategory("Forensic/Linux", "Linux", "icon", "desc", "comp2");

        Map<String, Object> stats = registry.getStatistics();
        assertNotNull(stats);
        assertTrue((Integer) stats.get("total_categories") >= 5); // includes parents
        assertEquals(2, stats.get("root_categories"));
    }

    @Test
    public void testCategoryMerge() {
        // Component 1 creates hierarchy
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp1");

        // Component 2 adds to same hierarchy
        registry.registerCategory("Forensic/Windows/EventLogs", "Event Logs", "icon", "desc", "comp2");

        // Both should coexist
        List<String> windowsSubcats = registry.getSubcategories("Forensic/Windows");
        assertEquals(2, windowsSubcats.size());
    }

    @Test
    public void testCategoryUpdate() {
        registry.registerCategory("Category/Test", "Old Name", "icon1", "old desc", "comp1");

        // Update with new info
        registry.registerCategory("Category/Test", "New Name", "icon2", "new desc", "comp1");

        FileCategoryRegistry.CategoryDescriptor updated = registry.getCategoryInfo("Category/Test");
        assertEquals("New Name", updated.displayName());
        assertEquals("icon2", updated.icon());
    }

    @Test
    public void testEmptyRegistry() {
        assertTrue(registry.getRootCategories().isEmpty());
        assertTrue(registry.getAllCategories().isEmpty());
        assertTrue(registry.getLeafCategories().isEmpty());
    }

    @Test
    public void testNullCategoryRetreval() {
        assertNull(registry.getCategory("NonExistent/Path"));
    }

    @Test
    public void testMultipleRootsAndBranches() {
        registry.registerCategory("Evidence/Physical/Devices", "Devices", "icon", "desc", "comp1");
        registry.registerCategory("Evidence/Digital/Logs", "Logs", "icon", "desc", "comp2");
        registry.registerCategory("Forensic/Windows/Registry", "Registry", "icon", "desc", "comp3");

        List<String> roots = registry.getRootCategories();
        assertEquals(2, roots.size()); // Evidence, Forensic

        List<String> allCats = registry.getAllCategories();
        assertTrue(allCats.size() >= 6); // multiple levels created
    }
}
