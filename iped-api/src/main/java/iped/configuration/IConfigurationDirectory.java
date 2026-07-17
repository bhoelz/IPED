package iped.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/**
 * Resolves configuration resources across an ordered list of lookup folders, allowing
 * profile-specific files to override the default configuration.
 */
public interface IConfigurationDirectory {

  /** System property holding the path of the active configuration directory. */
  String IPED_CONF_PATH = "iped.configPath";

  /** System property holding the IPED installation root folder. */
  String IPED_ROOT = "iped.root";

  /** System property holding the application root folder. */
  String IPED_APP_ROOT = "iped.app.root";

  /**
   * Adds a folder to the resource lookup path.
   *
   * @param path folder to search for configuration resources
   */
  void addPath(Path path);

  /**
   * @return the ordered list of folders searched for configuration resources
   */
  List<Path> getResourceLookupFolders();

  /**
   * Searches the lookup folders for resources matching the given predicate.
   *
   * @param predicate filter applied to each candidate path
   * @return matching resource paths, possibly empty
   * @throws IOException if a lookup folder cannot be read
   */
  List<Path> lookUpResource(Predicate<Path> predicate) throws IOException;

  /**
   * Searches the lookup folders for the resources used by the given configurable object.
   *
   * @param configurable the configurable whose resource filter is applied
   * @return matching resource paths, possibly empty
   * @throws IOException if a lookup folder cannot be read
   */
  List<Path> lookUpResource(Configurable<?> configurable) throws IOException;
}
