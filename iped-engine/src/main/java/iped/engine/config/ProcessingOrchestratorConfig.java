package iped.engine.config;

/**
 * Configuration for multi-case processing orchestration.
 *
 * Allows configuration of resource limits, concurrency levels, and scheduling
 * behavior for the ProcessingOrchestrator.
 */
public class ProcessingOrchestratorConfig {

    private int maxConcurrentCases = Runtime.getRuntime().availableProcessors();
    private long maxMemoryPerCase = Runtime.getRuntime().maxMemory() / maxConcurrentCases;
    private int sharedThreadPoolSize = Runtime.getRuntime().availableProcessors();

    public ProcessingOrchestratorConfig() {
    }

    public int getMaxConcurrentCases() {
        return maxConcurrentCases;
    }

    public void setMaxConcurrentCases(int maxConcurrentCases) {
        this.maxConcurrentCases = maxConcurrentCases;
    }

    public long getMaxMemoryPerCase() {
        return maxMemoryPerCase;
    }

    public void setMaxMemoryPerCase(long maxMemoryPerCase) {
        this.maxMemoryPerCase = maxMemoryPerCase;
    }

    public int getSharedThreadPoolSize() {
        return sharedThreadPoolSize;
    }

    public void setSharedThreadPoolSize(int sharedThreadPoolSize) {
        this.sharedThreadPoolSize = sharedThreadPoolSize;
    }
}
