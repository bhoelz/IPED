package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlobalStatsDto {
    private int activeCases;
    private int maxConcurrentCases;
    private long totalMemoryUsed;
    private long maxMemoryPerCase;
    private long totalItemsProcessed;

    public GlobalStatsDto() {
    }

    public int getActiveCases() {
        return activeCases;
    }

    public void setActiveCases(int activeCases) {
        this.activeCases = activeCases;
    }

    public int getMaxConcurrentCases() {
        return maxConcurrentCases;
    }

    public void setMaxConcurrentCases(int maxConcurrentCases) {
        this.maxConcurrentCases = maxConcurrentCases;
    }

    public long getTotalMemoryUsed() {
        return totalMemoryUsed;
    }

    public void setTotalMemoryUsed(long totalMemoryUsed) {
        this.totalMemoryUsed = totalMemoryUsed;
    }

    public long getMaxMemoryPerCase() {
        return maxMemoryPerCase;
    }

    public void setMaxMemoryPerCase(long maxMemoryPerCase) {
        this.maxMemoryPerCase = maxMemoryPerCase;
    }

    public long getTotalItemsProcessed() {
        return totalItemsProcessed;
    }

    public void setTotalItemsProcessed(long totalItemsProcessed) {
        this.totalItemsProcessed = totalItemsProcessed;
    }
}
