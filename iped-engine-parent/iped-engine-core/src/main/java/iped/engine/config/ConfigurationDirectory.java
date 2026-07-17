package iped.engine.config;

import iped.configuration.Configurable;
import iped.configuration.IConfigurationDirectory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConfigurationDirectory implements IConfigurationDirectory {

  /** Classpath directory where each module ships its built-in default configs. */
  public static final String DEFAULTS_RESOURCE_DIR = "iped/config/defaults"; // $NON-NLS-1$

  /** Marker resource enumerated to discover modules shipping default configs. */
  public static final String DEFAULTS_INDEX_RESOURCE =
      "META-INF/iped/config-defaults.idx"; //$NON-NLS-1$

  /**
   * Lowest precedence layer: built-in defaults shipped on the classpath by each module and configs
   * inside plugin JARs. Local directories always override these.
   */
  private List<Path> defaultDirs = new ArrayList<Path>();

  /**
   * Local layers in increasing precedence order: app conf dir, selected profile, case profile.
   * Within properties-based configurables the merge is key-level last-wins, so these only need to
   * contain deviations from the defaults.
   */
  private List<Path> configDirs = new ArrayList<Path>();

  public ConfigurationDirectory(Path configDir) {
    configDirs.add(configDir);
  }

  @Override
  public List<Path> getResourceLookupFolders() {
    List<Path> all = new ArrayList<Path>(defaultDirs);
    all.addAll(configDirs);
    return all;
  }

  @Override
  public List<Path> lookUpResource(Predicate<Path> predicate) throws IOException {
    final List<Path> result = new ArrayList<Path>();

    Consumer<Path> consumer =
        new Consumer<Path>() {
          @Override
          public void accept(Path t) {
            result.add(t);
          }
        };
    for (Path path : getResourceLookupFolders()) {
      // local config files/dirs are optional: only deviations from the
      // built-in defaults need to exist
      if (!Files.exists(path)) {
        continue;
      }
      Files.walk(path).filter(predicate).forEach(consumer);
    }

    return result;
  }

  public void addPath(Path path) {
    configDirs.add(path);
  }

  /** Adds a directory to the lowest precedence (defaults) layer. */
  public void addDefaultsPath(Path path) {
    defaultDirs.add(path);
  }

  /**
   * Mounts a plugin JAR/ZIP and adds its root to the defaults layer, so local configuration files
   * and profiles can override plugin shipped configs.
   */
  public void addZip(Path zip) throws IOException {
    defaultDirs.add(zipRoot(zip));
  }

  private static Path zipRoot(Path zip) throws IOException {
    zip = zip.normalize();

    URI uri;
    try {
      uri = new URI("jar:" + zip.toUri().toString());
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }

    FileSystem fs;
    try {
      fs = FileSystems.getFileSystem(uri);

    } catch (FileSystemNotFoundException e) {
      Map<String, String> env = new HashMap<String, String>();
      env.put("create", "false");
      if (zip.endsWith(".jar")) {
        env.put("multi-release", "true");
      }
      fs = FileSystems.newFileSystem(uri, env);
    }
    return fs.getRootDirectories().iterator().next();
  }

  /**
   * Discovers every module on the classpath shipping built-in default configs (marked by {@value
   * #DEFAULTS_INDEX_RESOURCE}) and registers their {@value #DEFAULTS_RESOURCE_DIR} directory in the
   * defaults layer.
   */
  public void addClasspathDefaults(ClassLoader classLoader) throws IOException {
    Enumeration<URL> markers = classLoader.getResources(DEFAULTS_INDEX_RESOURCE);
    while (markers.hasMoreElements()) {
      URL marker = markers.nextElement();
      Path defaultsDir = resolveDefaultsDir(marker);
      if (defaultsDir != null && Files.isDirectory(defaultsDir)) {
        defaultDirs.add(defaultsDir);
      }
    }
  }

  private static Path resolveDefaultsDir(URL marker) throws IOException {
    try {
      URI uri = marker.toURI();
      if ("jar".equals(uri.getScheme())) { // $NON-NLS-1$
        String spec = uri.getSchemeSpecificPart();
        int sep = spec.indexOf("!/"); // $NON-NLS-1$
        Path jar = Paths.get(new URI(spec.substring(0, sep)));
        return zipRoot(jar).resolve(DEFAULTS_RESOURCE_DIR);
      }
      if ("file".equals(uri.getScheme())) { // $NON-NLS-1$
        // exploded classes dir (dev/test): <root>/META-INF/iped/config-defaults.idx
        Path root = Paths.get(uri).getParent().getParent().getParent();
        return root.resolve(DEFAULTS_RESOURCE_DIR.replace('/', java.io.File.separatorChar));
      }
      return null;
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<Path> lookUpResource(Configurable<?> configurable) throws IOException {
    final DirectoryStream.Filter<Path> filter = configurable.getResourceLookupFilter();

    return lookUpResource(
        new Predicate<Path>() {
          @Override
          public boolean test(Path path) {
            try {
              return filter.accept(path);
            } catch (IOException e) {
              return false;
            }
          }
        });
  }
}
