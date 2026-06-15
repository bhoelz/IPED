package iped.runner.web;

import iped.runner.execution.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;
    private final RunHistoryService runHistory;
    private final RunQueueService runQueueService;

    /**
     * Enqueues an IPED run.
     *
     * <p>Body: {@code {"tokens":[...],"priority":"NORMAL","profile":"mobile"}}
     * Returns: {@code {"id":"<uuid>","queued":true}} — or {@code "queued":false}
     * if a slot was available and the process started immediately.
     */
    @PostMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> startRun(@RequestBody RunRequest req) {
        RunPriority priority = req.priority() != null ? req.priority() : RunPriority.NORMAL;
        QueuedRun qr = runQueueService.enqueue(req, priority);
        return ResponseEntity.ok(Map.of(
                "id",      qr.id(),
                "queued",  true,
                "priority", priority.name()
        ));
    }

    /**
     * Returns the current status of a run.
     * Active runs return {@code status: "running"}; terminal runs are looked up in history.
     */
    @GetMapping("/run/{id}")
    public ResponseEntity<Map<String, Object>> getRunStatus(@PathVariable String id) {
        var active = executionService.get(id);
        if (active.isPresent()) {
            RunRecord rec = active.get();
            return ResponseEntity.ok(Map.of(
                    "id", rec.id(),
                    "status", RunStatus.RUNNING.name().toLowerCase(),
                    "startedAt", rec.startedAt().toString()
            ));
        }
        return runHistory.get(id)
                .<ResponseEntity<Map<String, Object>>>map(s -> ResponseEntity.ok(summaryToMap(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns all historical runs (completed, failed, aborted), newest first.
     */
    @GetMapping("/runs")
    public ResponseEntity<List<RunSummary>> listRuns() {
        return ResponseEntity.ok(runHistory.all());
    }

    /**
     * SSE stream for a running job. Sends {@code log}, {@code done}, {@code error},
     * or {@code aborted} events.
     */
    @GetMapping(path = "/run/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRun(@PathVariable String id) {
        return executionService.get(id)
                .map(RunRecord::emitter)
                .orElseGet(() -> {
                    var emitter = new SseEmitter();
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("exitCode", -1, "message", "Run not found: " + id)));
                        emitter.complete();
                    } catch (Exception ignored) {}
                    return emitter;
                });
    }

    /**
     * Gracefully cancels a running job (SIGTERM → wait → SIGKILL).
     * Returns 404 if the run is not active (already finished or unknown).
     */
    @DeleteMapping("/run/{id}")
    public ResponseEntity<Void> abortRun(@PathVariable String id) {
        return executionService.abort(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static Map<String, Object> summaryToMap(RunSummary s) {
        return Map.of(
                "id", s.id(),
                "name", s.name(),
                "source", s.source(),
                "status", s.status().name().toLowerCase(),
                "startedAt", s.startedAt().toString(),
                "endedAt", s.endedAt() != null ? s.endedAt().toString() : Instant.now().toString(),
                "exitCode", s.exitCode(),
                "itemsProcessed", s.itemsProcessed(),
                "itemsFound", s.itemsFound()
        );
    }
}
