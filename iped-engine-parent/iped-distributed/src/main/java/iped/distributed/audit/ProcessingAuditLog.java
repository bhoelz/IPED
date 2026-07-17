package iped.distributed.audit;

import iped.distributed.status.ItemStatusEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * In-memory chain-of-custody audit log for the IPED distributed pipeline.
 *
 * <p>Accumulates {@link ProcessingRecord}s from the {@code iped.status} event stream. Only {@code
 * COMPLETED}, {@code ERROR}, and {@code TIMEOUT} events produce records; discovery and start events
 * are ignored.
 *
 * <h2>Persistence</h2>
 *
 * <p>When constructed with a non-null {@code auditLogFile}, every new record is also appended to
 * that file in newline-delimited CSV format. This provides a durable chain-of-custody log that
 * survives coordinator restarts. The file is opened in append mode so existing records are
 * preserved across restarts.
 *
 * <h2>Thread safety</h2>
 *
 * <p>Safe for concurrent reads and writes. File writes are serialised via {@code synchronized}.
 */
@Slf4j
public class ProcessingAuditLog {

  public static final String CSV_HEADER =
      "caseId,itemUuid,taskType,pipelineStage,agentId,processedAt,durationMs,outcome,errorMessage";

  private final ConcurrentHashMap<String, CopyOnWriteArrayList<ProcessingRecord>> byCaseId =
      new ConcurrentHashMap<>();

  private final Path auditLogFile; // null = in-memory only

  public ProcessingAuditLog() {
    this.auditLogFile = null;
  }

  /**
   * @param auditLogFile path to the CSV file for durable persistence; {@code null} for in-memory
   *     only
   */
  public ProcessingAuditLog(Path auditLogFile) {
    this.auditLogFile = auditLogFile;
    if (auditLogFile != null) ensureHeader(auditLogFile);
  }

  // -----------------------------------------------------------------------

  /** Process a status event; a {@link ProcessingRecord} is created for terminal events. */
  public void record(ItemStatusEvent event) {
    ProcessingRecord rec = ProcessingRecord.from(event);
    if (rec == null) return;

    byCaseId.computeIfAbsent(rec.caseId(), id -> new CopyOnWriteArrayList<>()).add(rec);

    if (auditLogFile != null) appendToFile(rec);
  }

  /** All records for a case in insertion order. Empty list if unknown case. */
  public List<ProcessingRecord> forCase(String caseId) {
    CopyOnWriteArrayList<ProcessingRecord> list = byCaseId.get(caseId);
    return list != null ? new ArrayList<>(list) : Collections.emptyList();
  }

  /**
   * All records for a specific item within a case. A single item may have multiple records (one per
   * task stage that produced a terminal event).
   */
  public List<ProcessingRecord> forItem(String caseId, String itemUuid) {
    return forCase(caseId).stream()
        .filter(r -> itemUuid.equals(r.itemUuid()))
        .collect(Collectors.toList());
  }

  /** Most-recent terminal record for an item, or empty if not yet processed. */
  public Optional<ProcessingRecord> latestForItem(String caseId, String itemUuid) {
    List<ProcessingRecord> all = forItem(caseId, itemUuid);
    return all.isEmpty() ? Optional.empty() : Optional.of(all.get(all.size() - 1));
  }

  /** Number of records for a case. */
  public int totalRecords(String caseId) {
    CopyOnWriteArrayList<ProcessingRecord> list = byCaseId.get(caseId);
    return list != null ? list.size() : 0;
  }

  /** All known case IDs that have at least one record. */
  public List<String> caseIds() {
    return new ArrayList<>(byCaseId.keySet());
  }

  // -----------------------------------------------------------------------
  // CSV export
  // -----------------------------------------------------------------------

  /**
   * Export all records for a case in CSV format, suitable for forensic chain-of-custody reporting.
   * Returns an empty CSV (header only) if the case has no records.
   */
  public String exportCsv(String caseId) {
    List<ProcessingRecord> records = forCase(caseId);
    StringBuilder sb = new StringBuilder(CSV_HEADER).append('\n');
    for (ProcessingRecord r : records) {
      sb.append(csvField(r.caseId()))
          .append(',')
          .append(csvField(r.itemUuid()))
          .append(',')
          .append(csvField(r.taskType()))
          .append(',')
          .append(r.pipelineStage())
          .append(',')
          .append(csvField(r.agentId()))
          .append(',')
          .append(r.processedAt())
          .append(',')
          .append(r.durationMs())
          .append(',')
          .append(csvField(r.outcome()))
          .append(',')
          .append(csvField(r.errorMessage()))
          .append('\n');
    }
    return sb.toString();
  }

  // -----------------------------------------------------------------------
  // Internal helpers
  // -----------------------------------------------------------------------

  private static String csvField(String s) {
    if (s == null) return "";
    if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }

  private void ensureHeader(Path file) {
    try {
      boolean exists = Files.exists(file);
      if (file.getParent() != null) Files.createDirectories(file.getParent());
      if (!exists) {
        Files.writeString(
            file, CSV_HEADER + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
      }
    } catch (IOException e) {
      log.warn("Could not initialise audit log file {}: {}", file, e.getMessage());
    }
  }

  private synchronized void appendToFile(ProcessingRecord r) {
    if (auditLogFile == null) return;
    try (BufferedWriter w =
        Files.newBufferedWriter(
            auditLogFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE)) {
      w.write(
          csvField(r.caseId())
              + ","
              + csvField(r.itemUuid())
              + ","
              + csvField(r.taskType())
              + ","
              + r.pipelineStage()
              + ","
              + csvField(r.agentId())
              + ","
              + r.processedAt()
              + ","
              + r.durationMs()
              + ","
              + csvField(r.outcome())
              + ","
              + csvField(r.errorMessage()));
      w.newLine();
    } catch (IOException e) {
      log.warn("Could not append to audit log {}: {}", auditLogFile, e.getMessage());
    }
  }
}
