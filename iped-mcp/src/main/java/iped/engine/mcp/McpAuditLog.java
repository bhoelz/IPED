package iped.engine.mcp;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Immutable audit trail of every MCP tool invocation.
 *
 * <p>Records are kept in-memory and, when {@code auditLogPath} is configured,
 * appended line-by-line to a JSONL file for forensic-grade chain-of-custody.
 *
 * <p>Each entry captures: timestamp, tool name, a sanitised summary of arguments
 * (no raw evidence text), and the outcome (success | error).
 *
 * <p>This satisfies Phase 2 governance item 1 (immutable audit trail) while keeping
 * the implementation in-module and independent of iped-runner's {@code ProcessingAuditLog}.
 */
@Slf4j
public class McpAuditLog {

    public record Entry(
            Instant  timestamp,
            String   tool,
            String   argSummary,
            Outcome  outcome,
            String   errorMessage
    ) {
        public enum Outcome { SUCCESS, ERROR }
    }

    private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();
    private final Path auditFile;

    public McpAuditLog() {
        this.auditFile = null;
    }

    public McpAuditLog(Path auditFile) {
        this.auditFile = auditFile;
    }

    /** Records a successful tool invocation. */
    public void success(String tool, Map<String, Object> args) {
        record(new Entry(Instant.now(), tool, summarise(args), Entry.Outcome.SUCCESS, null));
    }

    /** Records a failed tool invocation. */
    public void error(String tool, Map<String, Object> args, String errorMessage) {
        record(new Entry(Instant.now(), tool, summarise(args), Entry.Outcome.ERROR, errorMessage));
    }

    /** Returns an unmodifiable view of all entries, oldest first. */
    public List<Entry> all() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /** Returns the most recent {@code n} entries. */
    public List<Entry> recent(int n) {
        List<Entry> all = all();
        int from = Math.max(0, all.size() - n);
        return all.subList(from, all.size());
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void record(Entry e) {
        entries.add(e);
        log.info("mcp-audit tool={} outcome={} args={}", e.tool(), e.outcome(), e.argSummary());
        persist(e);
    }

    private void persist(Entry e) {
        if (auditFile == null) return;
        String line = toJsonLine(e);
        try (BufferedWriter w = Files.newBufferedWriter(auditFile,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException ex) {
            log.warn("Failed to persist MCP audit entry: {}", ex.getMessage());
        }
    }

    private static String toJsonLine(Entry e) {
        String err = e.errorMessage() != null
                ? ",\"error\":" + jsonString(e.errorMessage())
                : "";
        return "{\"ts\":\"" + e.timestamp()
                + "\",\"tool\":" + jsonString(e.tool())
                + ",\"args\":" + jsonString(e.argSummary())
                + ",\"outcome\":\"" + e.outcome() + "\""
                + err + "}";
    }

    /** Builds a safe, non-sensitive summary of tool arguments for the audit log. */
    static String summarise(Map<String, Object> args) {
        if (args == null || args.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> kv : args.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(jsonString(kv.getKey())).append(':');
            // Redact potentially large or sensitive argument values (query text,
            // content) — record only the type so the log remains compact and safe.
            Object v = kv.getValue();
            if (v instanceof String s) {
                // Redact long strings that could contain evidence text
                sb.append(s.length() > 200
                        ? jsonString("[string:" + s.length() + "chars]")
                        : jsonString(s));
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append(jsonString("[" + (v != null ? v.getClass().getSimpleName() : "null") + "]"));
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                      .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
