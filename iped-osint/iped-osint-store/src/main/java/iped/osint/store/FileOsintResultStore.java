package iped.osint.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import iped.osint.core.OsintResultStore;
import iped.osint.spi.OsintResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FileOsintResultStore implements OsintResultStore {

    private final Path root;
    private final Path resultsDir;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public FileOsintResultStore(Path root) throws IOException {
        this.root = root;
        this.resultsDir = root.resolve("results");
        Files.createDirectories(resultsDir);
    }

    @Override
    public synchronized Optional<OsintResult> findLatestByFingerprint(String pluginId, Integer itemId, String fingerprint) throws IOException {
        try (Stream<Path> stream = Files.list(resultsDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readQuietly)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(result -> result.pluginId().equals(pluginId))
                    .filter(result -> java.util.Objects.equals(result.itemId(), itemId))
                    .filter(result -> result.fingerprint().equals(fingerprint))
                    .max(Comparator.comparing(OsintResult::finishedAt));
        }
    }

    @Override
    public synchronized void save(OsintResult result) throws IOException {
        mapper.writeValue(file(result.executionId()).toFile(), result);
    }

    @Override
    public synchronized Optional<OsintResult> get(String executionId) throws IOException {
        Path file = file(executionId);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(mapper.readValue(file.toFile(), OsintResult.class));
    }

    @Override
    public synchronized List<OsintResult> list(String sourceId, Integer itemId, String pluginId, int limit) throws IOException {
        try (Stream<Path> stream = Files.list(resultsDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readQuietly)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(result -> sourceId == null || sourceId.equals(result.sourceId()))
                    .filter(result -> itemId == null || itemId.equals(result.itemId()))
                    .filter(result -> pluginId == null || pluginId.equals(result.pluginId()))
                    .sorted(Comparator.comparing(OsintResult::finishedAt).reversed())
                    .limit(limit <= 0 ? 50 : limit)
                    .toList();
        }
    }

    private Optional<OsintResult> readQuietly(Path path) {
        try {
            return Optional.of(mapper.readValue(path.toFile(), OsintResult.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Path file(String executionId) {
        return resultsDir.resolve(executionId + ".json");
    }
}
