package iped.distributed.kafka;

import iped.distributed.coordinator.CaseLifecycleManager;
import iped.distributed.config.DistributedConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Coordinator-side background service that automatically requeues DLQ items whose
 * {@code attempt} count is below the configured {@link DistributedConfig#getDlqAutoRetryMaxAttempts()}
 * threshold.
 *
 * <p>On each sweep the retrier:
 * <ol>
 *   <li>Lists all running cases from {@link CaseLifecycleManager}.</li>
 *   <li>For each case, peeks at DLQ entries.</li>
 *   <li>Requeues those with {@code attempt < maxAttempts} using the configured delay
 *       ({@link DistributedConfig#getDlqAutoRetryDelayMs()}).</li>
 * </ol>
 *
 * <p>Disabled when {@code dlqAutoRetryMaxAttempts = 0} (the default).  The coordinator
 * schedules {@link #sweep()} via its periodic executor.
 *
 * <p>Thread-safe: each sweep is a short-lived DLQ consumer operation.
 */
@Slf4j
public class DlqAutoRetrier {

    private final DlqManager            dlqManager;
    private final CaseLifecycleManager  lifecycle;
    private final int                   maxAttempts;
    private final long                  delayMs;

    public DlqAutoRetrier(DlqManager dlqManager, CaseLifecycleManager lifecycle,
                          DistributedConfig cfg) {
        this.dlqManager  = dlqManager;
        this.lifecycle   = lifecycle;
        this.maxAttempts = cfg.getDlqAutoRetryMaxAttempts();
        this.delayMs     = cfg.getDlqAutoRetryDelayMs();
    }

    /** Returns {@code true} when auto-retry is enabled (maxAttempts > 0). */
    public boolean isEnabled() {
        return maxAttempts > 0;
    }

    /**
     * Sweeps all RUNNING cases and requeues eligible DLQ items.
     * Called periodically by the coordinator's scheduler.
     */
    public void sweep() {
        if (!isEnabled()) return;

        List<String> runningCases = lifecycle.allCases().stream()
                .filter(s -> s.state == CaseLifecycleManager.CaseStatus.State.RUNNING)
                .map(s -> s.caseId)
                .collect(Collectors.toList());

        for (String caseId : runningCases) {
            try {
                sweepCase(caseId);
            } catch (Exception e) {
                log.warn("DLQ auto-retry sweep failed for case '{}': {}", caseId, e.getMessage());
            }
        }
    }

    private void sweepCase(String caseId) throws Exception {
        List<DlqEntry> entries = dlqManager.list(caseId, DlqManager.DEFAULT_LIST_LIMIT);
        if (entries.isEmpty()) return;

        List<DlqPosition> eligible = entries.stream()
                .filter(e -> e.getAttempt() < maxAttempts)
                .map(e -> new DlqPosition(e.getDlqTopic(), e.getPartition(), e.getOffset()))
                .collect(Collectors.toList());

        if (eligible.isEmpty()) return;

        log.info("DLQ auto-retry: requeuing {}/{} items for case '{}' (attempt < {}, delayMs={})",
                eligible.size(), entries.size(), caseId, maxAttempts, delayMs);
        dlqManager.requeue(caseId, eligible, delayMs);
    }
}
