package iped.engine.webapi.json;

import io.swagger.annotations.ApiModelProperty;

public class CaseStatusJSON {
    private String caseId;
    private String state;
    private long itemsProcessed;
    private long bytesProcessed;
    private long runtimeSeconds;
    private long errorCount;
    private String estimatedCompletion;

    @ApiModelProperty
    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    @ApiModelProperty
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @ApiModelProperty
    public long getItemsProcessed() {
        return itemsProcessed;
    }

    public void setItemsProcessed(long itemsProcessed) {
        this.itemsProcessed = itemsProcessed;
    }

    @ApiModelProperty
    public long getBytesProcessed() {
        return bytesProcessed;
    }

    public void setBytesProcessed(long bytesProcessed) {
        this.bytesProcessed = bytesProcessed;
    }

    @ApiModelProperty
    public long getRuntimeSeconds() {
        return runtimeSeconds;
    }

    public void setRuntimeSeconds(long runtimeSeconds) {
        this.runtimeSeconds = runtimeSeconds;
    }

    @ApiModelProperty
    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    @ApiModelProperty
    public String getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(String estimatedCompletion) {
        this.estimatedCompletion = estimatedCompletion;
    }
}
