package iped.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/**
 * Compile-check tests for configuration interfaces. Each test instantiates a
 * minimal anonymous implementation, confirming the interface contract is
 * satisfiable and that JaCoCo counts the bytecode.
 */
class InterfaceCompileChecksTest {

    // ── EnabledInterface ─────────────────────────────────────────────────────

    @Test
    void enabledInterface_isImplementable_andBehavesCorrectly() {
        EnabledInterface ei = new EnabledInterface() {
            private boolean enabled = false;

            @Override
            public boolean isEnabled() {
                return enabled;
            }

            @Override
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        };

        assertFalse(ei.isEnabled());
        ei.setEnabled(true);
        assertTrue(ei.isEnabled());
    }

    // ── IConfigurationDirectory ──────────────────────────────────────────────

    @Test
    void iConfigurationDirectory_constants_haveExpectedValues() {
        assertEquals("iped.configPath", IConfigurationDirectory.IPED_CONF_PATH);
        assertEquals("iped.root", IConfigurationDirectory.IPED_ROOT);
        assertEquals("iped.app.root", IConfigurationDirectory.IPED_APP_ROOT);
    }

    @Test
    void iConfigurationDirectory_isImplementable() throws IOException {
        IConfigurationDirectory dir = new IConfigurationDirectory() {
            private final List<Path> paths = new java.util.ArrayList<>();

            @Override
            public void addPath(Path path) {
                paths.add(path);
            }

            @Override
            public List<Path> getResourceLookupFolders() {
                return paths;
            }

            @Override
            public List<Path> lookUpResource(Predicate<Path> predicate) {
                return List.of();
            }

            @Override
            public List<Path> lookUpResource(Configurable<?> configurable) {
                return List.of();
            }
        };

        dir.addPath(Paths.get("/conf"));
        assertEquals(1, dir.getResourceLookupFolders().size());
        assertNotNull(dir.lookUpResource(p -> true));
        assertNotNull(dir.lookUpResource((Configurable<?>) null));
    }

    // ── ObjectManager ────────────────────────────────────────────────────────

    @Test
    void objectManager_isImplementable() {
        ObjectManager<String> mgr = new ObjectManager<>() {
            private final Set<String> objects = new java.util.HashSet<>();

            @Override
            public Set<? extends String> findObjects(Class<? extends String> clazz) {
                return objects;
            }

            @Override
            public Set<String> findObjects(String className) {
                return Set.of();
            }

            @Override
            public Set<String> getObjects() {
                return objects;
            }

            @Override
            public void addObject(String obj) {
                objects.add(obj);
            }

            @Override
            public void removeObject(String obj) {
                objects.remove(obj);
            }
        };

        assertTrue(mgr.getObjects().isEmpty());
        mgr.addObject("plugin-a");
        mgr.addObject("plugin-b");
        assertEquals(2, mgr.getObjects().size());
        mgr.removeObject("plugin-a");
        assertEquals(1, mgr.getObjects().size());
        assertFalse(mgr.findObjects(String.class).isEmpty());
        assertTrue(mgr.findObjects("NonExistent").isEmpty());
    }
}
