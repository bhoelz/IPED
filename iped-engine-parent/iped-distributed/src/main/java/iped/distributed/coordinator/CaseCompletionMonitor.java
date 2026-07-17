package iped.distributed.coordinator;

import iped.distributed.status.ItemStatusEvent;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Watches the {@code iped.status} event flow and provides the two automatic detections of the
 * hardening phase:
 *
 * <ul>
 *   <li><b>Item timeout</b> — an item with a STARTED event but no terminal event (COMPLETED /
 *       SKIPPED / ERROR) within the timeout window gets a TIMEOUT event published, so operators and
 *       the dashboard can spot stuck agents.
 *   <li><b>Case completion</b> — when every discovered item (including sub-items) has completed the
 *       final pipeline stage, the case is marked completed and a single CASE_COMPLETED event is
 *       published.
 * </ul>
 *
 * <p>Items parked in a DLQ never complete the final stage and therefore hold the case open — by
 * design: a forensic case is only complete when every item was either processed or explicitly
 * retried/abandoned by the operator.
 *
 * <p>Feed events via {@link #onEvent} (e.g. from an {@link
 * iped.distributed.status.ItemStatusConsumer}) and call {@link #sweepTimeouts} periodically from a
 * scheduler.
 */
@Slf4j
public class CaseCompletionMonitor {

  private final CaseLifecycleManager lifecycle;
  private final Consumer<ItemStatusEvent> publisher;
  private final long timeoutMs;

  private final Map<String, CaseProgress> progress = new ConcurrentHashMap<>();

  public CaseCompletionMonitor(
      CaseLifecycleManager lifecycle,
      Consumer<ItemStatusEvent> publisher,
      long itemTimeoutSeconds) {
    this.lifecycle = lifecycle;
    this.publisher = publisher;
    this.timeoutMs = itemTimeoutSeconds * 1000L;
  }

  // -----------------------------------------------------------------------

  public void onEvent(ItemStatusEvent e) {
    applyEvent(e, false);
  }

  /**
   * Rebuilds in-memory progress by replaying historical status events — typically the full {@code
   * iped.status} topic read from the beginning when a coordinator restarts after a crash.
   *
   * <p>Side effects are <b>suppressed</b> during replay: reconstructing the log must not re-publish
   * {@code CASE_COMPLETED} events or re-fire timeouts. A case that already emitted {@code
   * CASE_COMPLETED} in the history is recognised as complete (its {@code completedPublished} flag
   * is set) so it is not re-announced. After recovery, resume live processing by feeding new events
   * to {@link #onEvent}.
   *
   * <p>Replay is safe to run exactly once against an empty monitor: the counters and in-flight map
   * are reconstructed to the same state the pre-crash coordinator held, minus any events that were
   * still in flight on the wire at crash time (those are re-delivered to agents and re-emitted as
   * fresh events after recovery).
   *
   * @param history status events in topic order (oldest first)
   */
  public void recover(Iterable<ItemStatusEvent> history) {
    int count = 0;
    for (ItemStatusEvent e : history) {
      applyEvent(e, true);
      count++;
    }
    log.info(
        "Recovered completion progress from {} replayed status event(s) across {} case(s)",
        count,
        progress.size());
  }

  /**
   * Applies a single historical event in replay (side-effect-suppressed) mode. Streaming
   * counterpart of {@link #recover} for feeding a {@link
   * iped.distributed.status.StatusTopicReplayer} record-by-record.
   */
  public void recoverSingle(ItemStatusEvent e) {
    applyEvent(e, true);
  }

  private void applyEvent(ItemStatusEvent e, boolean replay) {
    if (e.getType() == null || e.getCaseId() == null) return;
    CaseProgress cp = progress.computeIfAbsent(e.getCaseId(), id -> new CaseProgress());
    cp.lastEventAtMs.set(System.currentTimeMillis());

    switch (e.getType()) {
      case DISCOVERED, SUBITEM_DISCOVERED -> cp.discovered.incrementAndGet();

      case STARTED ->
          cp.inFlight.put(
              flightKey(e),
              new Flight(
                  e.getItemUuid(),
                  e.getTaskType(),
                  e.getPipelineStage(),
                  e.getTimestamp() != null ? e.getTimestamp() : Instant.now()));

      case COMPLETED -> {
        cp.inFlight.remove(flightKey(e));
        if (isFinalStage(e)) {
          cp.completedFinal.incrementAndGet();
          checkCompletion(e.getCaseId(), cp, replay);
        }
      }

      case SKIPPED -> cp.inFlight.remove(flightKey(e));

      case ERROR -> {
        cp.inFlight.remove(flightKey(e));
        cp.failedCount.incrementAndGet();
      }

      case CASE_COMPLETED -> cp.completedPublished = true;

      default -> {
        /* TIMEOUT events are produced, not consumed, here */
      }
    }
  }

  /**
   * Publishes a TIMEOUT event for every in-flight item older than the timeout window. Call
   * periodically (e.g. every 30s). Each stuck item is reported once.
   */
  public void sweepTimeouts(Instant now) {
    progress.forEach(
        (caseId, cp) ->
            cp.inFlight
                .entrySet()
                .removeIf(
                    entry -> {
                      Flight f = entry.getValue();
                      long elapsed = now.toEpochMilli() - f.startedAt.toEpochMilli();
                      if (elapsed < timeoutMs) return false;

                      log.warn(
                          "Item '{}' timed out in task '{}' stage {} of case '{}' ({}s)",
                          f.itemUuid,
                          f.taskType,
                          f.stage,
                          caseId,
                          elapsed / 1000);
                      publisher.accept(
                          ItemStatusEvent.timeout(
                              caseId, f.itemUuid, f.taskType, f.stage, elapsed));
                      return true;
                    }));
  }

  // ---- Progress accessors (dashboard / failover verification) -------------

  /** Number of items (incl. sub-items) discovered for a case; 0 if unknown. */
  public long discoveredCount(String caseId) {
    CaseProgress cp = progress.get(caseId);
    return cp != null ? cp.discovered.get() : 0;
  }

  /** Number of items that have passed the final pipeline stage; 0 if unknown. */
  public long completedFinalCount(String caseId) {
    CaseProgress cp = progress.get(caseId);
    return cp != null ? cp.completedFinal.get() : 0;
  }

  /** Number of items currently in flight (STARTED, no terminal event yet). */
  public int inFlightCount(String caseId) {
    CaseProgress cp = progress.get(caseId);
    return cp != null ? cp.inFlight.size() : 0;
  }

  /** True once the case has been (or was previously) announced as complete. */
  public boolean isCompleted(String caseId) {
    CaseProgress cp = progress.get(caseId);
    return cp != null && cp.completedPublished;
  }

  /** Number of ERROR-terminal events seen for items in this case; 0 if unknown. */
  public long failedCount(String caseId) {
    CaseProgress cp = progress.get(caseId);
    return cp != null ? cp.failedCount.get() : 0;
  }

  /**
   * Returns {@code true} when a case appears stalled: items were discovered but not all completed,
   * there are no items currently in flight, and no event has arrived for at least {@code
   * stallWindowMs} milliseconds. Returns {@code false} for unknown cases, already-completed cases,
   * or cases with zero discovered items.
   */
  public boolean isStalled(String caseId, long stallWindowMs) {
    CaseProgress cp = progress.get(caseId);
    if (cp == null) return false;
    long discovered = cp.discovered.get();
    long completed = cp.completedFinal.get();
    if (discovered == 0 || completed >= discovered || cp.completedPublished) return false;
    if (!cp.inFlight.isEmpty()) return false;
    long silenceMs = System.currentTimeMillis() - cp.lastEventAtMs.get();
    return silenceMs >= stallWindowMs;
  }

  /** Epoch-millis of the last status event seen for a case; 0 if unknown. */
  public long lastEventAtMs(String caseId) {
    CaseProgress cp = progress.get(caseId);
    return cp != null ? cp.lastEventAtMs.get() : 0L;
  }

  // -----------------------------------------------------------------------

  private void checkCompletion(String caseId, CaseProgress cp, boolean replay) {
    long found = cp.discovered.get();
    if (cp.completedPublished || found == 0 || cp.completedFinal.get() < found) return;

    synchronized (cp) {
      if (cp.completedPublished) return;
      cp.completedPublished = true;
    }
    if (replay) {
      // During recovery we reconstruct the "completed" flag silently; the original
      // CASE_COMPLETED was already published by the pre-crash coordinator (and will
      // also appear later in the replayed history). Do not re-announce.
      log.debug(
          "Case '{}' recognised as already complete during recovery ({} items)", caseId, found);
      return;
    }
    log.info("Case '{}' completed: all {} items passed the final stage", caseId, found);
    lifecycle.completeCase(caseId);
    publisher.accept(ItemStatusEvent.caseCompleted(caseId));
  }

  private boolean isFinalStage(ItemStatusEvent e) {
    try {
      return e.getPipelineStage() >= lifecycle.getTaskOrder(e.getCaseId()).size() - 1;
    } catch (IllegalArgumentException unknownCase) {
      // Case not registered with this coordinator (e.g. replayed history) — be safe.
      return false;
    }
  }

  private static String flightKey(ItemStatusEvent e) {
    return e.getItemUuid() + "|" + e.getTaskType();
  }

  // ---- Internal state -----------------------------------------------------

  private static class CaseProgress {
    final AtomicLong discovered = new AtomicLong();
    final AtomicLong completedFinal = new AtomicLong();
    final AtomicLong failedCount = new AtomicLong();
    final AtomicLong lastEventAtMs = new AtomicLong(System.currentTimeMillis());
    final Map<String, Flight> inFlight = new ConcurrentHashMap<>();
    volatile boolean completedPublished;
  }

  private record Flight(String itemUuid, String taskType, int stage, Instant startedAt) {}
}
