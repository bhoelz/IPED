package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Maps {@code GET /v2/jobs/{id}} — async job status from iped-webapi. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDto {
  private String id;
  private String type;
  private String status; // pending | running | completed | failed | cancelled
  private int progress; // 0–100
  private String message;
  private String createdAt;
  private String terminatedAt; // null while running

  public String getId() {
    return id;
  }

  public void setId(String v) {
    id = v;
  }

  public String getType() {
    return type;
  }

  public void setType(String v) {
    type = v;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String v) {
    status = v;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress(int v) {
    progress = v;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String v) {
    message = v;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String v) {
    createdAt = v;
  }

  public String getTerminatedAt() {
    return terminatedAt;
  }

  public void setTerminatedAt(String v) {
    terminatedAt = v;
  }
}
