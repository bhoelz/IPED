package iped.runner.execution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the history of terminal runs (COMPLETED / FAILED / ABORTED).
 * Persists to a JSON file in the runner work directory so history survives restarts.
 */
@Service
@Slf4j
public class RunHistoryService {

    @Value("${runner.history-file:run-history.json}")
    private String historyFile;

    private final ConcurrentHashMap<String, RunSummary> history = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    private Path historyPath;

    public RunHistoryService() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    void load() {
        historyPath = Path.of(historyFile);
        if (!Files.exists(historyPath)) {
            log.info("No run history file at {}; starting fresh", historyPath.toAbsolutePath());
            return;
        }
        try {
            List<RunSummary> list = mapper.readValue(historyPath.toFile(),
                    new TypeReference<List<RunSummary>>() {});
            for (RunSummary s : list) {
                history.put(s.id(), s);
            }
            log.info("Loaded {} run history record(s) from {}", history.size(), historyPath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not load run history from {}: {}", historyPath, e.getMessage());
        }
    }

    /** Records a terminal run and persists to disk. */
    public void record(RunSummary summary) {
        history.put(summary.id(), summary);
        persist();
    }

    public Optional<RunSummary> get(String id) {
        return Optional.ofNullable(history.get(id));
    }

    /** Returns all history entries, newest first. */
    public List<RunSummary> all() {
        var list = new ArrayList<>(history.values());
        list.sort((a, b) -> b.startedAt().compareTo(a.startedAt()));
        return Collections.unmodifiableList(list);
    }

    private void persist() {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(historyPath.toFile(), history.values());
        } catch (IOException e) {
            log.warn("Could not persist run history to {}: {}", historyPath, e.getMessage());
        }
    }
}
