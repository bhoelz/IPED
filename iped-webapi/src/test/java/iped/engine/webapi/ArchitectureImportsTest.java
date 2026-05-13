package iped.engine.webapi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ArchitectureImportsTest {
    @Test
    void webApiMustNotImportEngineModuleClassesDirectly() throws IOException {
        Path src = Path.of("src", "main", "java");
        try (Stream<Path> paths = Files.walk(src)) {
            List<String> forbidden = paths.filter(p -> p.toString().endsWith(".java"))
                    .flatMap(this::readLinesSafe)
                    .map(String::trim)
                    .filter(line -> line.startsWith("import iped.engine."))
                    .filter(line -> !line.startsWith("import iped.engine.webapi."))
                    .filter(line -> !line.equals("import iped.engine.Version;"))
                    .toList();
            assertTrue(forbidden.isEmpty(), "Forbidden engine imports found: " + forbidden);
        }
    }

    private Stream<String> readLinesSafe(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException e) {
            return Stream.of();
        }
    }
}
