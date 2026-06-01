package iped.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConfigurableDefaultMethodsTest {

    @Test
    void processConfigs_iteratesAllProvidedPaths() throws IOException {
        List<Path> visited = new ArrayList<>();

        Configurable<Void> configurable = new Configurable<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public DirectoryStream.Filter<Path> getResourceLookupFilter() {
                return p -> true;
            }

            @Override
            public void processConfig(Path resource) {
                visited.add(resource);
            }

            @Override
            public Void getConfiguration() {
                return null;
            }

            @Override
            public void setConfiguration(Void config) {
            }
        };

        Path p1 = Paths.get("a.conf");
        Path p2 = Paths.get("b.conf");
        configurable.processConfigs(List.of(p1, p2));

        assertEquals(2, visited.size());
        assertEquals(p1, visited.get(0));
        assertEquals(p2, visited.get(1));
    }

    @Test
    void processConfigs_emptyList_doesNothing() throws IOException {
        List<Path> visited = new ArrayList<>();

        Configurable<Void> configurable = new Configurable<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public DirectoryStream.Filter<Path> getResourceLookupFilter() {
                return p -> true;
            }

            @Override
            public void processConfig(Path resource) {
                visited.add(resource);
            }

            @Override
            public Void getConfiguration() {
                return null;
            }

            @Override
            public void setConfiguration(Void config) {
            }
        };

        configurable.processConfigs(List.of());

        assertEquals(0, visited.size());
    }
}
