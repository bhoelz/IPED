package iped.distributed.coordinator;

import iped.distributed.status.ItemStatusEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Watches the {@code iped.status} event flow and provides the two automatic
 * detections of the hardening phase:
 *
 * <ul>
 *   <li><b>Item timeout</b> — an item with a STARTED event but no terminal event
 *       (COMPLETED / SKIPPED / ERROR) within the timeout window gets a TIMEOUT
 *       event published, so operators and the dashboard can spot stuck agents.</li>
 *   <li><b>Case completion</b> — when every discovered item (including sub-items)
 *       has completed the final pipeline stage, the case is marked completed and a
 *       single CASE_COMPLETED event is published.</li>
 * </ul>
 *
 * <p>Items parked in a DLQ never complete the final stage and therefore hold the
 * case open — by design: a forensic case is only complete when every item was
 * either processed or explicitly retried/abandoned by the operator.
 *
 * <p>Feed events via {@link #onEvent} (e.g. from an
 * {@link iped.distributed.status.ItemStatusConsumer}) and call
 * {@link #sweepTimeouts} periodically from a scheduler.
 */
@Slf4j
public class CaseCompletionMonitor {

    private final CaseLifecycleManager lifecycle;
    private final Consumer<ItemStatusEvent> publisher;
    private final long timeoutMs;

    private final Map<String, CaseProgress> progress = new ConcurrentHashMap<>();

    public CaseCompletionMonitor(CaseLifecycleManager lifecycle,
                                 Consumer<ItemStatusEvent> publisher,
                                 long itemTimeoutSeconds) {
        this.lifecycle = lifecycle;
        this.publisher = publisher;
        this.timeoutMs = itemTimeoutSeconds * 1000L;
    }

    // -----------------------------------------------------------------------

    public void onEvent(ItemStatusEvent e) {
        if (e.getType() == null || e.getCaseId() == null) return;
        CaseProgress cp = progress.computeIfAbsent(e.getCaseId(), id -> new CaseProgress());

        switch (e.getType()) {
            case DISCOVERED, SUBITEM_DISCOVERED -> cp.discovered.incrementAndGet();

            case STARTED -> cp.inFlight.put(flightKey(e),
                    new Flight(e.getItemUuid(), e.getTaskType(), e.getPipelineStage(),
                            e.getTimestamp() != null ? e.getTimestamp() : Instant.now()));

            case COMPLETED -> {
                cp.inFlight.remove(flightKey(e));
                if (isFinalStage(e)) {
                    cp.completedFinal.incrementAndGet();
                    checkCompletion(e.getCaseId(), cp);
                }
            }

            case SKIPPED, ERROR -> cp.inFlight.remove(flightKey(e));

            case CASE_COMPLETED -> cp.completedPublished = true;

            default -> { /* TIMEOUT events are produced, not consumed, here */ }
        }
    }

    /**
     * Publishes a TIMEOUT event for every in-flight item older than the timeout
     * window. Call periodically (e.g. every 30s). Each stuck item is reported once.
     */
    public void sweepTimeouts(Instant now) {
        progress.forEach((caseId, cp) ->
                cp.inFlight.entrySet().removeIf(entry -> {
                    Flight f = entry.getValue();
                    long elapsed = now.toEpochMilli() - f.startedAt.toEpochMilli();
                    if (elapsed < timeoutMs) return false;

                    log.warn("Item '{}' timed out in task '{}' stage {} of case '{}' ({}s)",
                            f.itemUuid, f.taskType, f.stage, caseId, elapsed / 1000);
                    publisher.accept(ItemStatusEvent.timeout(
                            caseId, f.itemUuid, f.taskType, f.stage, elapsed));
                    return true;
                }));
    }

    // -----------------------------------------------------------------------

    private void checkCompletion(String caseId, CaseProgress cp) {
        long found = cp.discovered.get();
        if (cp.completedPublished || found == 0 || cp.completedFinal.get() < found) return;

        synchronized (cp) {
            if (cp.completedPublished) return;
            cp.completedPublished = true;
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
        final AtomicLong discovered     = new AtomicLong();
        final AtomicLong completedFinal = new AtomicLong();
        final Map<String, Flight> inFlight = new ConcurrentHashMap<>();
        volatile boolean completedPublished;
    }

    private record Flight(String itemUuid, String taskType, int stage, Instant startedAt) {}
}
