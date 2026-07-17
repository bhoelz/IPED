package iped.distributed.coordinator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Aggregated availability for all agents of a given task type. Returned by {@code GET
 * /api/v1/agents/availability}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentAvailability {

  private String taskType;
  private int stageNumber;
  private int totalAgents;
  private int totalSlots;
  private int freeSlots;
  private int busySlots;

  public static AgentAvailability of(
      String taskType, int stageNumber, int totalAgents, int totalSlots, int freeSlots) {
    AgentAvailability a = new AgentAvailability();
    a.taskType = taskType;
    a.stageNumber = stageNumber;
    a.totalAgents = totalAgents;
    a.totalSlots = totalSlots;
    a.freeSlots = freeSlots;
    a.busySlots = totalSlots - freeSlots;
    return a;
  }

  public String getTaskType() {
    return taskType;
  }

  public int getStageNumber() {
    return stageNumber;
  }

  public int getTotalAgents() {
    return totalAgents;
  }

  public int getTotalSlots() {
    return totalSlots;
  }

  public int getFreeSlots() {
    return freeSlots;
  }

  public int getBusySlots() {
    return busySlots;
  }

  public void setTaskType(String v) {
    taskType = v;
  }

  public void setStageNumber(int v) {
    stageNumber = v;
  }

  public void setTotalAgents(int v) {
    totalAgents = v;
  }

  public void setTotalSlots(int v) {
    totalSlots = v;
  }

  public void setFreeSlots(int v) {
    freeSlots = v;
  }

  public void setBusySlots(int v) {
    busySlots = v;
  }
}
