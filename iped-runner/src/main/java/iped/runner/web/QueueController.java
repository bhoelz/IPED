package iped.runner.web;

import iped.runner.execution.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * REST endpoints for the run queue and batch submission.
 *
 * <h2>Routes</h2>
 * <pre>
 * GET  /queue             — queued (not yet running) runs
 * GET  /profiles          — available TOML profile names
 * POST /runs/batch        — scan a directory and enqueue one run per evidence item
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class QueueController {

    private final RunQueueService queueService;

    /** Returns all runs waiting in the queue (not yet started). */
    @GetMapping("/queue")
    public ResponseEntity<List<Map<String, Object>>> listQueue() {
        var items = queueService.pending().stream()
                .map(QueueController::toMap)
                .toList();
        return ResponseEntity.ok(items);
    }

    /** Returns available TOML profile names from the profiles directory. */
    @GetMapping("/profiles")
    public ResponseEntity<List<String>> listProfiles() {
        return ResponseEntity.ok(queueService.availableProfiles());
    }

    /**
     * Batch-submit: scans {@code sourceDir} for immediate children and enqueues
     * one run per child using the provided {@code tokens} as a template.
     *
     * <p>Request body:
     * <pre>
     * {
     *   "sourceDir": "/cases/batch-2025",
     *   "outputDir": "/iped-output",
     *   "priority":  "NORMAL",      // optional, default NORMAL
     *   "profile":   "mobile",      // optional; adds --profile &lt;name&gt;
     *   "extraTokens": [{"flag":"-Xmx","val":"8g"}]  // optional
     * }
     * </pre>
     *
     * <p>Each child directory/file under {@code sourceDir} becomes one IPED run:
     * {@code -d <child> -o <outputDir>/<child-name>}.
     */
    @PostMapping(path = "/runs/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> batchSubmit(@RequestBody Map<String, Object> body) {
        String sourceDir = (String) body.get("sourceDir");
        String outputDir = (String) body.get("outputDir");
        if (sourceDir == null || outputDir == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'sourceDir' and 'outputDir' are required"));
        }

        Path src = Path.of(sourceDir);
        if (!Files.exists(src)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "sourceDir does not exist: " + sourceDir));
        }

        RunPriority priority = parsePriority((String) body.get("priority"));
        String profile = (String) body.get("profile");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> extraRaw =
                body.containsKey("extraTokens") ? (List<Map<String, String>>) body.get("extraTokens") : List.of();
        List<RunRequest.RunToken> extraTokens = extraRaw.stream()
                .map(m -> new RunRequest.RunToken(m.get("flag"), m.get("val")))
                .toList();

        List<String> enqueued = new ArrayList<>();
        List<String> errors   = new ArrayList<>();

        try (Stream<Path> children = Files.list(src)) {
            children.sorted().forEach(child -> {
                try {
                    var tokens = new ArrayList<RunRequest.RunToken>();
                    tokens.add(new RunRequest.RunToken("-d", child.toString()));
                    tokens.add(new RunRequest.RunToken("-o",
                            Path.of(outputDir, child.getFileName().toString()).toString()));
                    if (profile != null && !profile.isBlank()) {
                        tokens.add(new RunRequest.RunToken("--profile", profile));
                    }
                    tokens.addAll(extraTokens);

                    var qr = queueService.enqueue(new RunRequest(tokens, priority, profile), priority);
                    enqueued.add(qr.id());
                } catch (Exception e) {
                    errors.add(child.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list sourceDir: " + e.getMessage()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enqueuedCount", enqueued.size());
        result.put("enqueuedIds", enqueued);
        if (!errors.isEmpty()) result.put("errors", errors);
        return ResponseEntity.ok(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static RunPriority parsePriority(String s) {
        if (s == null) return RunPriority.NORMAL;
        try {
            return RunPriority.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RunPriority.NORMAL;
        }
    }

    private static Map<String, Object> toMap(QueuedRun r) {
        return Map.of(
                "id",          r.id(),
                "priority",    r.priority().name(),
                "enqueuedAt",  r.enqueuedAt().toString(),
                "profile",     r.request().profile() != null ? r.request().profile() : "",
                "name",        runName(r.request())
        );
    }

    private static String runName(RunRequest req) {
        if (req == null || req.tokens() == null) return "IPED Job";
        return req.tokens().stream()
                .filter(t -> "-d".equals(t.flag()) || "--datasource".equals(t.flag()))
                .map(t -> Path.of(t.val()).getFileName().toString())
                .findFirst()
                .orElse("IPED Job");
    }
}
