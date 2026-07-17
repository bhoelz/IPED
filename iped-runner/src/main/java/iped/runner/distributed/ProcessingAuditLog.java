package iped.runner.distributed;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-memory chain-of-custody audit log for processed items.
 *
 * <p>Records are keyed by {@code caseId → [ProcessingRecord…]} in arrival order. All reads and
 * appends are thread-safe. When {@code auditLogPath} is configured (non-blank) each record is also
 * appended to an append-mode CSV file so the log survives runner restarts.
 *
 * <p>CSV columns (header written on first write per file):
 *
 * <pre>
 * itemUuid,caseId,taskType,pipelineStage,agentId,processedAt,durationMs,outcome,errorMessage
 * </pre>
 */
@Component
@Slf4j
public class ProcessingAuditLog {

  private final ConcurrentHashMap<String, CopyOnWriteArrayList<ProcessingRecord>> store =
      new ConcurrentHashMap<>();

  private final Path auditLogFile;
  private volatile boolean headerWritten = false;

  public ProcessingAuditLog() {
    this.auditLogFile = null;
  }

  public ProcessingAuditLog(Path auditLogFile) {
    this.auditLogFile = auditLogFile;
  }

  /**
   * Appends a record to the in-memory store and, if configured, the CSV file. Silently ignores
   * {@code null} records (non-terminal events).
   */
  public void record(ProcessingRecord r) {
    if (r == null) return;
    store.computeIfAbsent(r.caseId(), k -> new CopyOnWriteArrayList<>()).add(r);
    persistCsv(r);
  }

  /** All records for {@code caseId} in arrival order (never null, may be empty). */
  public List<ProcessingRecord> forCase(String caseId) {
    var list = store.get(caseId);
    return list != null ? List.copyOf(list) : List.of();
  }

  /** All records for a specific item across its processing pipeline stages. */
  public List<ProcessingRecord> forItem(String caseId, String itemUuid) {
    return forCase(caseId).stream().filter(r -> itemUuid.equals(r.itemUuid())).toList();
  }

  /** The most recent terminal record for an item, or empty when none exists. */
  public Optional<ProcessingRecord> latestForItem(String caseId, String itemUuid) {
    return forItem(caseId, itemUuid).stream()
        .max(Comparator.comparing(ProcessingRecord::processedAt));
  }

  /**
   * Exports all records for {@code caseId} as CSV.
   *
   * <p>Returns an empty string when no records exist for that case.
   */
  public String exportCsv(String caseId) {
    List<ProcessingRecord> records = forCase(caseId);
    if (records.isEmpty()) return csvHeader();
    var sb = new StringBuilder(csvHeader());
    for (ProcessingRecord r : records) {
      sb.append(csvRow(r));
    }
    return sb.toString();
  }

  // ── CSV helpers ──────────────────────────────────────────────────────────

  private static final String CSV_HEADER =
      "itemUuid,caseId,taskType,pipelineStage,agentId,processedAt,durationMs,outcome,errorMessage\n";

  private static String csvHeader() {
    return CSV_HEADER;
  }

  private static String csvRow(ProcessingRecord r) {
    return String.join(
            ",",
            csvEscape(r.itemUuid()),
            csvEscape(r.caseId()),
            csvEscape(r.taskType()),
            String.valueOf(r.pipelineStage()),
            csvEscape(r.agentId()),
            r.processedAt() != null ? DateTimeFormatter.ISO_INSTANT.format(r.processedAt()) : "",
            String.valueOf(r.durationMs()),
            r.outcome() != null ? r.outcome().name() : "",
            csvEscape(r.errorMessage()))
        + "\n";
  }

  private static String csvEscape(String s) {
    if (s == null || s.isBlank()) return "";
    if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }

  private void persistCsv(ProcessingRecord r) {
    if (auditLogFile == null) return;
    try {
      boolean fileExists = Files.exists(auditLogFile);
      try (BufferedWriter w =
          Files.newBufferedWriter(
              auditLogFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        if (!fileExists && !headerWritten) {
          w.write(csvHeader());
          headerWritten = true;
        }
        w.write(csvRow(r));
      }
    } catch (IOException e) {
      log.warn("Failed to persist audit record to {}: {}", auditLogFile, e.getMessage());
    }
  }
}
