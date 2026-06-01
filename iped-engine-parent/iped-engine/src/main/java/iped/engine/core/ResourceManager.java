package iped.engine.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages resource allocation and enforcement for multi-case processing.
 *
 * Tracks memory usage per case and enforces quotas to prevent resource exhaustion
 * when processing multiple cases concurrently.
 */
public class ResourceManager {

    private final long maxMemoryPerCase;
    private final int maxConcurrentCases;

    private final ConcurrentHashMap<UUID, Long> caseMemoryUsage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> casePaused = new ConcurrentHashMap<>();
    private final AtomicLong totalMemoryUsed = new AtomicLong(0);

    public ResourceManager(long maxMemoryPerCase, int maxConcurrentCases) {
        this.maxMemoryPerCase = maxMemoryPerCase;
        this.maxConcurrentCases = maxConcurrentCases;
    }

    /**
     * Acquire resources for a case. Returns immediately if resources available,
     * blocks or throws if limits exceeded.
     *
     * @param caseId the case UUID
     * @throws IllegalStateException if max concurrent cases exceeded
     */
    public synchronized void acquireResources(UUID caseId) {
        int activeCases = (int) caseMemoryUsage.keySet().stream()
                .filter(id -> !casePaused.getOrDefault(id, false))
                .count();

        if (activeCases >= maxConcurrentCases) {
            throw new IllegalStateException(
                    "Max concurrent cases (" + maxConcurrentCases + ") exceeded");
        }

        caseMemoryUsage.put(caseId, 0L);
        casePaused.put(caseId, false);
    }

    /**
     * Release resources for a case after processing completes.
     *
     * @param caseId the case UUID
     */
    public synchronized void releaseResources(UUID caseId) {
        Long memoryUsed = caseMemoryUsage.remove(caseId);
        if (memoryUsed != null) {
            totalMemoryUsed.addAndGet(-memoryUsed);
        }
        casePaused.remove(caseId);
    }

    /**
     * Update memory usage for a case.
     *
     * @param caseId the case UUID
     * @param bytesUsed bytes currently used by this case
     */
    public void updateMemoryUsage(UUID caseId, long bytesUsed) {
        Long previousUsage = caseMemoryUsage.get(caseId);
        if (previousUsage != null) {
            long delta = bytesUsed - previousUsage;
            caseMemoryUsage.put(caseId, bytesUsed);
            totalMemoryUsed.addAndGet(delta);
        }
    }

    /**
     * Check if quotas are exceeded and pause/resume cases as needed.
     *
     * @return true if any cases were paused due to quota exceedance
     */
    public synchronized boolean enforceQuotas() {
        boolean anyPaused = false;
        long memoryUsed = totalMemoryUsed.get();
        long availableMemory = Runtime.getRuntime().maxMemory();

        for (UUID caseId : caseMemoryUsage.keySet()) {
            long caseUsage = caseMemoryUsage.get(caseId);
            boolean shouldPause = caseUsage > maxMemoryPerCase
                    || memoryUsed > (availableMemory * 0.9);

            Boolean isPaused = casePaused.get(caseId);
            if (shouldPause && !isPaused) {
                casePaused.put(caseId, true);
                anyPaused = true;
            } else if (!shouldPause && isPaused) {
                casePaused.put(caseId, false);
            }
        }

        return anyPaused;
    }

    /**
     * Check if a specific case is paused.
     *
     * @param caseId the case UUID
     * @return true if case is paused
     */
    public boolean isPaused(UUID caseId) {
        return casePaused.getOrDefault(caseId, false);
    }

    /**
     * Get memory usage for a specific case.
     *
     * @param caseId the case UUID
     * @return bytes used by the case
     */
    public long getMemoryUsage(UUID caseId) {
        return caseMemoryUsage.getOrDefault(caseId, 0L);
    }

    /**
     * Get total memory used across all cases.
     *
     * @return total bytes used
     */
    public long getTotalMemoryUsage() {
        return totalMemoryUsed.get();
    }

    /**
     * Get count of active (non-paused) cases.
     *
     * @return number of active cases
     */
    public int getActiveCaseCount() {
        return (int) caseMemoryUsage.keySet().stream()
                .filter(id -> !casePaused.getOrDefault(id, false))
                .count();
    }

    public long getMaxMemoryPerCase() {
        return maxMemoryPerCase;
    }

    public int getMaxConcurrentCases() {
        return maxConcurrentCases;
    }
}
