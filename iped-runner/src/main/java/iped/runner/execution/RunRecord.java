package iped.runner.execution;

import java.time.Instant;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Snapshot of a single in-flight IPED process and its SSE channel. */
public record RunRecord(
    String id, Process process, SseEmitter emitter, Instant startedAt, RunRequest request) {}
