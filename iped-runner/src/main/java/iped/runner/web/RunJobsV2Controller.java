package iped.runner.web;

import iped.runner.execution.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Control-plane adapter — exposes the runner's run state using the same JSON
 * shape as {@code iped-webapi}'s {@code /v2/jobs} resource.
 *
 * <p>This lets the MCP {@code job_*} tools and the web UI talk to either service
 * with an identical contract. Active runs map to status {@code "running"}; queued
 * runs map to {@code "pending"}; terminal history entries map to
 * {@code "completed"}, {@code "failed"}, or {@code "cancelled"}.
 *
 * <h2>Routes</h2>
 * <pre>
 * GET    /v2/jobs          — list (active runs + pending queue + recent history)
 * GET    /v2/jobs/{id}     — single job by run-id
 * DELETE /v2/jobs/{id}     — cancel a running/queued job (no-op for terminal)
 * </pre>
 *
 * <p>POST is intentionally omitted — run submission goes through {@code POST /run}
 * or {@code POST /runs/batch} which carry runner-specific richer metadata.
 */
@RestController
@RequiredArgsConstructor
public class RunJobsV2Controller {

    private final ExecutionService  executionService;
    private final RunQueueService   runQueueService;
    private final RunHistoryService runHistory;

    @GetMapping("/v2/jobs")
    public List<Map<String, Object>> listJobs() {
        var jobs = new ArrayList<Map<String, Object>>();

        // 1. Running
        executionService.snapshots().forEach(snap ->
                jobs.add(snapshotToJob(snap)));

        // 2. Queued (pending)
        runQueueService.pending().forEach(q ->
                jobs.add(queuedToJob(q)));

        // 3. History (terminal), newest first
        runHistory.all().forEach(s ->
                jobs.add(summaryToJob(s)));

        // De-duplicate by id (active wins over history if id appears in both)
        var seen  = new LinkedHashSet<String>();
        var dedup = new ArrayList<Map<String, Object>>();
        for (var job : jobs) {
            if (seen.add((String) job.get("id"))) dedup.add(job);
        }
        return dedup;
    }

    @GetMapping("/v2/jobs/{id}")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable String id) {
        // Active run?
        var active = executionService.get(id);
        if (active.isPresent()) {
            var snap = executionService.snapshots().stream()
                    .filter(s -> s.id().equals(id)).findFirst();
            if (snap.isPresent()) return ResponseEntity.ok(snapshotToJob(snap.get()));
            // Still alive but not yet in snapshots — return minimal running shape
            return ResponseEntity.ok(Map.of(
                    "id", id, "type", "iped-run", "status", "running",
                    "progress", 0, "message", ""));
        }

        // Queued?
        var queued = runQueueService.pending().stream()
                .filter(q -> q.id().equals(id)).findFirst();
        if (queued.isPresent()) return ResponseEntity.ok(queuedToJob(queued.get()));

        // History?
        return runHistory.get(id)
                .<ResponseEntity<Map<String, Object>>>map(s -> ResponseEntity.ok(summaryToJob(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/v2/jobs/{id}")
    public ResponseEntity<Void> cancelJob(@PathVariable String id) {
        // Try to abort an active run first.
        if (executionService.abort(id)) return ResponseEntity.noContent().build();

        // If it's queued but not yet started, there is no direct abort API on the queue.
        // History entries are already terminal — treat as no-op (204).
        if (runHistory.get(id).isPresent()) return ResponseEntity.noContent().build();

        return ResponseEntity.notFound().build();
    }

    // ── Converters ────────────────────────────────────────────────────────────

    private static Map<String, Object> snapshotToJob(JobSnapshot snap) {
        int progress = 0;
        long processed = snap.itemsProcessed();
        long found     = snap.itemsFound();
        if (found > 0) {
            progress = (int) Math.min(99, Math.round(100.0 * processed / found));
        }

        var m = new LinkedHashMap<String, Object>();
        m.put("id",        snap.id());
        m.put("type",      "iped-run");
        m.put("status",    "running");
        m.put("progress",  progress);
        m.put("message",   snap.recentLines() != null && !snap.recentLines().isEmpty()
                ? snap.recentLines().get(snap.recentLines().size() - 1) : "");
        m.put("createdAt", snap.startedAt().toString());
        m.put("params",    Map.of("name", snap.name(), "source", snap.source()));
        return m;
    }

    private static Map<String, Object> queuedToJob(QueuedRun q) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id",        q.id());
        m.put("type",      "iped-run");
        m.put("status",    "pending");
        m.put("progress",  0);
        m.put("message",   "Queued (priority=" + q.priority().name() + ")");
        m.put("createdAt", q.enqueuedAt().toString());
        m.put("params",    Map.of(
                "priority", q.priority().name(),
                "profile",  q.request().profile() != null ? q.request().profile() : ""));
        return m;
    }

    private static Map<String, Object> summaryToJob(RunSummary s) {
        String status = switch (s.status()) {
            case COMPLETED -> "completed";
            case FAILED    -> "failed";
            case ABORTED, TIMED_OUT -> "cancelled";
            case RUNNING   -> "running";
            case QUEUED    -> "pending";
        };

        int progress = s.status() == RunStatus.COMPLETED ? 100 : 0;

        var m = new LinkedHashMap<String, Object>();
        m.put("id",            s.id());
        m.put("type",          "iped-run");
        m.put("status",        status);
        m.put("progress",      progress);
        m.put("message",       "exit=" + s.exitCode());
        m.put("createdAt",     s.startedAt() != null ? s.startedAt().toString() : Instant.now().toString());
        if (s.endedAt() != null) m.put("terminatedAt", s.endedAt().toString());
        m.put("params", Map.of(
                "name",           s.name(),
                "source",         s.source(),
                "itemsProcessed", s.itemsProcessed(),
                "itemsFound",     s.itemsFound()));
        return m;
    }
}
