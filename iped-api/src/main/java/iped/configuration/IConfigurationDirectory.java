package iped.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public interface IConfigurationDirectory {

    String IPED_CONF_PATH = "iped.configPath";

    String IPED_ROOT = "iped.root";

    String IPED_APP_ROOT = "iped.app.root";

    void addPath(Path path);

    List<Path> getResourceLookupFolders();

    List<Path> lookUpResource(Predicate<Path> predicate) throws IOException;

    List<Path> lookUpResource(Configurable<?> configurable) throws IOException;
}
