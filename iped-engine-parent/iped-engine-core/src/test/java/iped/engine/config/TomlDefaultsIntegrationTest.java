package iped.engine.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the core guarantee of the TOML migration: every configuration can
 * be loaded purely from the built-in classpath defaults (zero-config boot),
 * and local files only need to contain deviating keys.
 */
class TomlDefaultsIntegrationTest {

    @TempDir
    Path tempDir;

    private ConfigurationDirectory newDirectoryWithDefaults() throws Exception {
        ConfigurationDirectory dir = new ConfigurationDirectory(tempDir.resolve("LocalConfig.toml"));
        dir.addClasspathDefaults(getClass().getClassLoader());
        return dir;
    }

    @Test
    void fileSystemConfigLoadsFromClasspathDefaultsOnly() throws Exception {
        ConfigurationDirectory dir = newDirectoryWithDefaults();
        FileSystemConfig config = new FileSystemConfig();
        List<Path> resources = dir.lookUpResource(config);
        assertFalse(resources.isEmpty(), "built-in default FileSystemConfig.toml not found on classpath");
        config.processConfigs(resources);

        assertTrue(config.isRobustImageReading());
        assertEquals(1L << 30, config.getUnallocatedFragSize());
        assertEquals(-1L, config.getMinOrphanSizeToIgnore());
        assertTrue(config.isIgnoreHardLinks());
        assertFalse(config.isToAddUnallocated());
        assertFalse(config.isToAddFileSlacks());
        assertEquals("", config.getSkipFolderRegex());
    }

    @Test
    void enableTaskPropertyReadsDefaultIPEDConfig() throws Exception {
        ConfigurationDirectory dir = newDirectoryWithDefaults();
        EnableTaskProperty enableHash = new EnableTaskProperty("enableHash");
        List<Path> resources = dir.lookUpResource(enableHash);
        assertFalse(resources.isEmpty(), "built-in default IPEDConfig.toml not found on classpath");
        enableHash.processConfigs(resources);
        assertTrue(enableHash.isEnabled());

        EnableTaskProperty enableOcr = new EnableTaskProperty("enableOCR");
        enableOcr.processConfigs(dir.lookUpResource(enableOcr));
        assertFalse(enableOcr.isEnabled());
    }

    @Test
    void localDeviationOverridesSingleKeyKeepingOtherDefaults() throws Exception {
        Path conf = Files.createDirectories(tempDir.resolve("conf"));
        Files.write(conf.resolve("FileSystemConfig.toml"),
                "addUnallocated = true\n".getBytes(StandardCharsets.UTF_8));

        ConfigurationDirectory dir = newDirectoryWithDefaults();
        dir.addPath(conf);

        FileSystemConfig config = new FileSystemConfig();
        config.processConfigs(dir.lookUpResource(config));

        // deviation applied
        assertTrue(config.isToAddUnallocated());
        // defaults for everything else kept
        assertTrue(config.isRobustImageReading());
        assertTrue(config.isIgnoreHardLinks());
        assertEquals(1L << 30, config.getUnallocatedFragSize());
    }

    @Test
    void profileLayerWinsOverLocalConf() throws Exception {
        Path conf = Files.createDirectories(tempDir.resolve("conf"));
        Files.write(conf.resolve("FileSystemConfig.toml"),
                "unallocatedFragSize = 100\n".getBytes(StandardCharsets.UTF_8));
        Path profileConf = Files.createDirectories(tempDir.resolve("profiles/test/conf"));
        Files.write(profileConf.resolve("FileSystemConfig.toml"),
                "unallocatedFragSize = 200\n".getBytes(StandardCharsets.UTF_8));

        ConfigurationDirectory dir = newDirectoryWithDefaults();
        dir.addPath(conf);
        dir.addPath(profileConf);

        FileSystemConfig config = new FileSystemConfig();
        config.processConfigs(dir.lookUpResource(config));
        assertEquals(200L, config.getUnallocatedFragSize());
        assertTrue(config.isRobustImageReading());
    }
}
