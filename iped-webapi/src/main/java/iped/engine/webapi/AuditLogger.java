package iped.engine.webapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight audit log — one JSON object per line (JSONL) written to the file
 * named by {@code iped.webapi.audit-log} system property (default:
 * {@code audit.jsonl} in the working directory).
 *
 * <p>Every mutating HTTP operation should call {@link #log(String, Map)} via
 * {@link AuditLoggingFilter}. Direct callers (e.g. {@link CasesV2}) can also
 * call it for business-level events.
 */
public final class AuditLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogger.class);

    private static final Path LOG_FILE = Paths.get(
            System.getProperty("iped.webapi.audit-log", "audit.jsonl"));

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AuditLogger() {}

    /**
     * Appends one JSONL record.
     *
     * @param action  short verb, e.g. {@code "case.open"}, {@code "case.close"},
     *                {@code "source.add"}
     * @param details arbitrary key/value pairs included in the record
     */
    public static void log(String action, Map<String, ?> details) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("ts", Instant.now().toString());
        record.put("action", action);
        record.putAll(details);

        try {
            String line = MAPPER.writeValueAsString(record) + System.lineSeparator();
            try (Writer w = Files.newBufferedWriter(LOG_FILE,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
            }
        } catch (IOException e) {
            LOG.warn("audit log write failed for action '{}': {}", action, e.getMessage());
        }

        // Always echo to SLF4J so it appears in the application log.
        LOG.info("AUDIT action={} details={}", action, details);
    }
}
