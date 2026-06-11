package iped.runner.web;

import iped.runner.execution.ExecutionService;
import iped.runner.execution.RunRecord;
import iped.runner.execution.RunRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * Starts an IPED process. Body: {@code {"tokens":[{"flag":"-d","val":"..."},...]}}
     * Returns: {@code {"id":"<uuid>"}}
     */
    @PostMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startRun(@RequestBody RunRequest req) {
        try {
            RunRecord rec = executionService.start(req);
            return ResponseEntity.ok(Map.of("id", rec.id()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
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

    /** Forcibly kills the IPED process for the given run ID. */
    @DeleteMapping("/run/{id}")
    public ResponseEntity<Void> abortRun(@PathVariable String id) {
        return executionService.abort(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
