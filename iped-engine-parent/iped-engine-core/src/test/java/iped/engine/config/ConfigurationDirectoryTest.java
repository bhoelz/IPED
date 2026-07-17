package iped.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationDirectoryTest {

  @TempDir Path tempDir;

  @Test
  void lookUpResource_skipsMissingPathsAndOrdersDefaultsFirst() throws IOException {
    Path localConf = Files.createDirectories(tempDir.resolve("conf"));
    Files.write(localConf.resolve("MyConfig.toml"), "a = 2".getBytes(StandardCharsets.UTF_8));

    Path defaults = Files.createDirectories(tempDir.resolve("defaults"));
    Files.write(defaults.resolve("MyConfig.toml"), "a = 1".getBytes(StandardCharsets.UTF_8));

    ConfigurationDirectory dir =
        new ConfigurationDirectory(tempDir.resolve("missing-LocalConfig.toml"));
    dir.addPath(localConf);
    dir.addDefaultsPath(defaults);

    List<Path> found =
        dir.lookUpResource(
            p -> p.getFileName() != null && p.getFileName().toString().equals("MyConfig.toml"));

    assertEquals(2, found.size());
    // defaults layer must come first so local files override it key by key
    assertEquals(defaults.resolve("MyConfig.toml"), found.get(0));
    assertEquals(localConf.resolve("MyConfig.toml"), found.get(1));
  }

  @Test
  void addClasspathDefaults_discoversExplodedClasspathEntry() throws IOException {
    Path root = Files.createDirectories(tempDir.resolve("classes"));
    Path metaInf = Files.createDirectories(root.resolve("META-INF/iped"));
    Files.write(
        metaInf.resolve("config-defaults.idx"),
        "iped/config/defaults/MyConfig.toml".getBytes(StandardCharsets.UTF_8));
    Path defaults = Files.createDirectories(root.resolve("iped/config/defaults"));
    Files.write(defaults.resolve("MyConfig.toml"), "a = 1".getBytes(StandardCharsets.UTF_8));

    ConfigurationDirectory dir = new ConfigurationDirectory(tempDir.resolve("missing.toml"));
    try (URLClassLoader cl = new URLClassLoader(new URL[] {root.toUri().toURL()}, null)) {
      dir.addClasspathDefaults(cl);
    }

    List<Path> found =
        dir.lookUpResource(
            p -> p.getFileName() != null && p.getFileName().toString().equals("MyConfig.toml"));
    assertEquals(1, found.size());
  }

  @Test
  void addClasspathDefaults_discoversJarEntryAndLocalOverrides() throws IOException {
    Path jar = tempDir.resolve("module.jar");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jar))) {
      zos.putNextEntry(new ZipEntry("META-INF/iped/config-defaults.idx"));
      zos.write("iped/config/defaults/MyConfig.toml".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry("iped/config/defaults/MyConfig.toml"));
      zos.write("a = 1".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    Path localConf = Files.createDirectories(tempDir.resolve("conf"));
    Files.write(localConf.resolve("MyConfig.toml"), "a = 2".getBytes(StandardCharsets.UTF_8));

    ConfigurationDirectory dir = new ConfigurationDirectory(tempDir.resolve("missing.toml"));
    try (URLClassLoader cl = new URLClassLoader(new URL[] {jar.toUri().toURL()}, null)) {
      dir.addClasspathDefaults(cl);
    }
    dir.addPath(localConf);

    List<Path> found =
        dir.lookUpResource(
            p -> p.getFileName() != null && p.getFileName().toString().equals("MyConfig.toml"));
    assertEquals(2, found.size());
    // jar defaults first, local deviation last (wins on key-level merge)
    assertTrue(found.get(0).toUri().toString().startsWith("jar:"));
    assertEquals(localConf.resolve("MyConfig.toml"), found.get(1));
  }
}
