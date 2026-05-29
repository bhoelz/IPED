package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseStatusDto {
    private String caseId;
    private String state;
    private long itemsProcessed;
    private long bytesProcessed;
    private long runtimeSeconds;
    private long errorCount;
    private long estimatedCompletion;

    public CaseStatusDto() {
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getItemsProcessed() {
        return itemsProcessed;
    }

    public void setItemsProcessed(long itemsProcessed) {
        this.itemsProcessed = itemsProcessed;
    }

    public long getBytesProcessed() {
        return bytesProcessed;
    }

    public void setBytesProcessed(long bytesProcessed) {
        this.bytesProcessed = bytesProcessed;
    }

    public long getRuntimeSeconds() {
        return runtimeSeconds;
    }

    public void setRuntimeSeconds(long runtimeSeconds) {
        this.runtimeSeconds = runtimeSeconds;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(long estimatedCompletion) {
        this.estimatedCompletion = estimatedCompletion;
    }
}
