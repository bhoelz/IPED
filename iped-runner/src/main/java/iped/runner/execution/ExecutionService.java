package iped.runner.execution;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class ExecutionService {

    @Value("${runner.executable:iped}")
    private String executable;

    private final ConcurrentHashMap<String, RunRecord> sessions  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RunStats>  jobStats  = new ConcurrentHashMap<>();
    private final List<SseEmitter> dashboardEmitters             = new CopyOnWriteArrayList<>();

    private final ExecutorService ioPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "runner-io");
        t.setDaemon(true);
        return t;
    });

    private final ScheduledExecutorService broadcastScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "runner-dashboard-broadcast");
                t.setDaemon(true);
                return t;
            });

    @PostConstruct
    void startBroadcast() {
        broadcastScheduler.scheduleAtFixedRate(this::broadcastUpdate, 2, 2, TimeUnit.SECONDS);
    }

    // -------------------------------------------------------------------------
    // Job lifecycle
    // -------------------------------------------------------------------------

    /**
     * Launches IPED with the provided token list and returns a RunRecord
     * containing the process handle and a live SSE emitter.
     */
    public RunRecord start(RunRequest req) throws IOException {
        var cmd = new ArrayList<String>();
        for (String part : executable.strip().split("\\s+")) cmd.add(part);
        for (var token : req.tokens()) {
            cmd.add(token.flag());
            if (token.val() != null && !token.val().isEmpty()) cmd.add(token.val());
        }
        log.info("Launching IPED: {}", cmd);

        var process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        var id = UUID.randomUUID().toString();
        var emitter = new SseEmitter(0L);
        var record  = new RunRecord(id, process, emitter, Instant.now(), req);
        var stats   = new RunStats();

        sessions.put(id, record);
        jobStats.put(id, stats);

        emitter.onCompletion(() -> sessions.remove(id));
        emitter.onTimeout(() -> sessions.remove(id));

        ioPool.submit(() -> pumpOutput(record, stats));
        broadcastNow();
        return record;
    }

    private void pumpOutput(RunRecord rec, RunStats stats) {
        try (var reader = new BufferedReader(new InputStreamReader(rec.process().getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stats.addLine(line);
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

        stats.status = code == 0 ? "done" : "error";
        log.info("Run {} exited with code {} (status: {})", rec.id(), code, stats.status);

        try {
            rec.emitter().send(SseEmitter.event()
                    .name(code == 0 ? "done" : "error")
                    .data(Map.of("exitCode", code)));
            rec.emitter().complete();
        } catch (Exception e) {
            log.debug("SSE completion send dropped for run {}: {}", rec.id(), e.getMessage());
        }

        broadcastNow();
    }

    public Optional<RunRecord> get(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    /** Forcibly terminates the process and sends an 'aborted' SSE event. */
    public boolean abort(String id) {
        var rec = sessions.remove(id);
        if (rec == null) return false;

        rec.process().destroyForcibly();
        var stats = jobStats.get(id);
        if (stats != null) stats.status = "aborted";

        try {
            rec.emitter().send(SseEmitter.event().name("aborted").data(Map.of()));
            rec.emitter().complete();
        } catch (Exception ignored) {}

        log.info("Run {} aborted", id);
        broadcastNow();
        return true;
    }

    // -------------------------------------------------------------------------
    // Dashboard API
    // -------------------------------------------------------------------------

    /** Returns a point-in-time snapshot of all known jobs (running and recently finished). */
    public List<JobSnapshot> snapshots() {
        // Include running sessions + stats-only entries for recently finished jobs
        var ids = new LinkedHashSet<String>();
        ids.addAll(sessions.keySet());
        ids.addAll(jobStats.keySet());

        return ids.stream()
                .map(id -> {
                    var rec   = sessions.get(id);
                    var stats = jobStats.getOrDefault(id, new RunStats());
                    var start = rec != null ? rec.startedAt() : Instant.now();
                    var req   = rec != null ? rec.request()   : null;
                    return new JobSnapshot(
                            id,
                            jobName(req),
                            jobSource(req),
                            stats.status,
                            start,
                            Duration.between(start, Instant.now()).toMillis(),
                            stats.itemsProcessed.get(),
                            stats.itemsFound.get(),
                            stats.currentSpeed.get(),
                            stats.eta.get(),
                            stats.tailLines(20)
                    );
                })
                .sorted(Comparator.comparing(JobSnapshot::startedAt))
                .toList();
    }

    /**
     * Returns an SSE emitter that receives a {@code jobs-update} event every 2 seconds
     * with the full current job list. Also fires immediately on job start/end.
     */
    public SseEmitter subscribeDashboard() {
        var emitter = new SseEmitter(0L);
        dashboardEmitters.add(emitter);
        emitter.onCompletion(() -> dashboardEmitters.remove(emitter));
        emitter.onTimeout(() -> dashboardEmitters.remove(emitter));
        // Send current state immediately so the client doesn't wait up to 2s
        try {
            emitter.send(SseEmitter.event().name("jobs-update").data(snapshots()));
        } catch (Exception ignored) {}
        return emitter;
    }

    // -------------------------------------------------------------------------
    // Internal broadcast helpers
    // -------------------------------------------------------------------------

    private void broadcastNow() {
        broadcastScheduler.submit(this::broadcastUpdate);
    }

    private void broadcastUpdate() {
        if (dashboardEmitters.isEmpty()) return;
        var payload = snapshots();
        List<SseEmitter> dead = new ArrayList<>();
        for (var emitter : dashboardEmitters) {
            try {
                emitter.send(SseEmitter.event().name("jobs-update").data(payload));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        dashboardEmitters.removeAll(dead);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String jobName(RunRequest req) {
        if (req == null) return "IPED Job";
        return req.tokens().stream()
                .filter(t -> "-d".equals(t.flag()) || "--datasource".equals(t.flag()))
                .map(t -> Path.of(t.val()).getFileName().toString())
                .findFirst()
                .orElse("IPED Job");
    }

    private static String jobSource(RunRequest req) {
        if (req == null) return "—";
        return req.tokens().stream()
                .filter(t -> "-d".equals(t.flag()) || "--datasource".equals(t.flag()))
                .map(RunRequest.RunToken::val)
                .findFirst()
                .orElse("—");
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down {} active run(s)", sessions.size());
        sessions.values().forEach(r -> r.process().destroyForcibly());
        ioPool.shutdownNow();
        broadcastScheduler.shutdownNow();
    }
}
