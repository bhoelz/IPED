package iped.runner.distributed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * Mirror of {@code iped.distributed.status.ItemStatusEvent} as published on the global {@code
 * iped.status} Kafka topic.
 *
 * <p>Deliberately duplicated instead of depending on {@code iped-distributed}: the runner stays
 * free of engine dependencies and the JSON schema of the topic is the contract between the two
 * modules. Unknown fields are ignored so the producer can evolve.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusEvent {

  public enum Type {
    DISCOVERED,
    STARTED,
    COMPLETED,
    SKIPPED,
    ERROR,
    TIMEOUT,
    SUBITEM_DISCOVERED,
    CASE_COMPLETED
  }

  private Type type;
  private String caseId;
  private String itemUuid;
  private String itemPath;
  private String taskType;
  private int pipelineStage;
  private String agentId;
  private Instant timestamp;
  private long durationMs;
  private String errorMessage;
  private String errorClass;

  public Type getType() {
    return type;
  }

  public void setType(Type v) {
    type = v;
  }

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String v) {
    caseId = v;
  }

  public String getItemUuid() {
    return itemUuid;
  }

  public void setItemUuid(String v) {
    itemUuid = v;
  }

  public String getItemPath() {
    return itemPath;
  }

  public void setItemPath(String v) {
    itemPath = v;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String v) {
    taskType = v;
  }

  public int getPipelineStage() {
    return pipelineStage;
  }

  public void setPipelineStage(int v) {
    pipelineStage = v;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String v) {
    agentId = v;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant v) {
    timestamp = v;
  }

  public long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(long v) {
    durationMs = v;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String v) {
    errorMessage = v;
  }

  public String getErrorClass() {
    return errorClass;
  }

  public void setErrorClass(String v) {
    errorClass = v;
  }
}
