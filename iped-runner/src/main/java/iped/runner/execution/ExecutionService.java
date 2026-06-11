package iped.runner.execution;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class ExecutionService {

    @Value("${runner.executable:iped}")
    private String executable;

    private final ConcurrentHashMap<String, RunRecord> sessions = new ConcurrentHashMap<>();
    private final ExecutorService ioPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "runner-io");
        t.setDaemon(true);
        return t;
    });

    /**
     * Launches IPED with the provided token list and returns a RunRecord
     * containing the process handle and a live SSE emitter.
     */
    public RunRecord start(RunRequest req) throws IOException {
        var cmd = new ArrayList<String>();
        for (String part : executable.strip().split("\\s+")) {
            cmd.add(part);
        }
        for (var token : req.tokens()) {
            cmd.add(token.flag());
            if (token.val() != null && !token.val().isEmpty()) cmd.add(token.val());
        }
        log.info("Launching IPED: {}", cmd);

        var process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        var id = UUID.randomUUID().toString();
        // 0 = no emitter timeout; the process lifetime drives completion
        var emitter = new SseEmitter(0L);
        var record = new RunRecord(id, process, emitter, Instant.now());
        sessions.put(id, record);

        emitter.onCompletion(() -> sessions.remove(id));
        emitter.onTimeout(() -> sessions.remove(id));

        ioPool.submit(() -> pumpOutput(record));
        return record;
    }

    private void pumpOutput(RunRecord rec) {
        try (var reader = new BufferedReader(new InputStreamReader(rec.process().getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    rec.emitter().send(SseEmitter.event()
                            .name("log")
                            .data(Map.of("line", line)));
                } catch (Exception e) {
                    log.debug("SSE send dropped for run {}: {}", rec.id(), e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            log.warn("I/O error reading process output for run {}: {}", rec.id(), e.getMessage());
        }

        int code;
        try {
            code = rec.process().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            code = -1;
        }

        log.info("Run {} exited with code {}", rec.id(), code);
        try {
            rec.emitter().send(SseEmitter.event()
                    .name(code == 0 ? "done" : "error")
                    .data(Map.of("exitCode", code)));
            rec.emitter().complete();
        } catch (Exception e) {
            log.debug("SSE completion send dropped for run {}: {}", rec.id(), e.getMessage());
        }
    }

    public Optional<RunRecord> get(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    /** Forcibly terminates the process and sends an 'aborted' SSE event. */
    public boolean abort(String id) {
        var rec = sessions.remove(id);
        if (rec == null) return false;

        rec.process().destroyForcibly();
        try {
            rec.emitter().send(SseEmitter.event().name("aborted").data(Map.of()));
            rec.emitter().complete();
        } catch (Exception ignored) {}

        log.info("Run {} aborted", id);
        return true;
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down {} active run(s)", sessions.size());
        sessions.values().forEach(r -> r.process().destroyForcibly());
        ioPool.shutdownNow();
    }
}
