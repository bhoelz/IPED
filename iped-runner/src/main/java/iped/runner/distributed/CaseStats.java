package iped.runner.distributed;

import iped.runner.execution.JobSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live aggregate of all {@link StatusEvent}s seen for one distributed case.
 *
 * <p>The pipeline depth is not known up-front, so progress is measured as items completed at the
 * highest pipeline stage observed so far — once every stage has been seen, that equals "items that
 * left the pipeline".
 */
class CaseStats {

  private final String caseId;
  private final Instant firstEvent;

  private volatile Instant lastEvent;
  private volatile boolean caseCompleted;
  private volatile int maxStage;

  private final AtomicLong discovered = new AtomicLong();
  private final AtomicLong errors = new AtomicLong();
  private final Map<Integer, AtomicLong> completedByStage = new ConcurrentHashMap<>();

  /** Recent noteworthy lines (errors/timeouts/lifecycle) shown in the dashboard log tab. */
  private final ArrayDeque<String> recentLines = new ArrayDeque<>(50);

  CaseStats(String caseId, Instant firstEvent) {
    this.caseId = caseId;
    this.firstEvent = firstEvent != null ? firstEvent : Instant.now();
    this.lastEvent = this.firstEvent;
  }

  void apply(StatusEvent e) {
    if (e.getTimestamp() != null) lastEvent = e.getTimestamp();

    switch (e.getType()) {
      case DISCOVERED, SUBITEM_DISCOVERED -> discovered.incrementAndGet();
      case COMPLETED -> {
        int stage = e.getPipelineStage();
        if (stage > maxStage) maxStage = stage;
        completedByStage.computeIfAbsent(stage, s -> new AtomicLong()).incrementAndGet();
      }
      case ERROR, TIMEOUT -> {
        errors.incrementAndGet();
        addLine(
            String.format(
                "[%s] %s stage=%d %s: %s",
                e.getType(),
                e.getTaskType(),
                e.getPipelineStage(),
                e.getItemPath(),
                e.getErrorMessage()));
      }
      case CASE_COMPLETED -> {
        caseCompleted = true;
        addLine("[CASE_COMPLETED] case " + caseId + " reached the final pipeline stage");
      }
      default -> {
        /* STARTED / SKIPPED don't change the aggregate view */
      }
    }
  }

  private synchronized void addLine(String line) {
    if (recentLines.size() >= 50) recentLines.pollFirst();
    recentLines.addLast(line);
  }

  private synchronized List<String> tailLines(int n) {
    var all = List.copyOf(recentLines);
    return all.subList(Math.max(0, all.size() - n), all.size());
  }

  JobSnapshot toSnapshot() {
    long found = discovered.get();
    var lastStage = completedByStage.get(maxStage);
    long processed = lastStage != null ? lastStage.get() : 0;

    Instant end = caseCompleted ? lastEvent : Instant.now();
    long duration = Duration.between(firstEvent, end).toMillis();

    String status = caseCompleted ? "done" : "running";
    String speed =
        duration > 1000 && processed > 0
            ? String.format("%d items/s", processed * 1000 / duration)
            : "—";

    return new JobSnapshot(
        "dist:" + caseId,
        caseId,
        "Kafka · distributed pipeline (stage " + maxStage + ")",
        status,
        firstEvent,
        duration,
        processed,
        found,
        speed,
        "—",
        tailLines(20));
  }

  long errorCount() {
    return errors.get();
  }
}
