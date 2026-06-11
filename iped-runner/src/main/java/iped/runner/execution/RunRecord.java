package iped.runner.execution;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.Instant;

/** Snapshot of a single in-flight IPED process and its SSE channel. */
public record RunRecord(
        String id,
        Process process,
        SseEmitter emitter,
        Instant startedAt,
        RunRequest request
) {}
