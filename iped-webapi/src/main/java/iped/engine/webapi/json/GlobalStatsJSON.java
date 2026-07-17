package iped.engine.webapi.json;

import io.swagger.annotations.ApiModelProperty;

public class GlobalStatsJSON {
  private int activeCases;
  private int maxConcurrentCases;
  private long totalMemoryUsed;
  private long maxMemoryPerCase;
  private long totalItemsProcessed;

  @ApiModelProperty
  public int getActiveCases() {
    return activeCases;
  }

  public void setActiveCases(int activeCases) {
    this.activeCases = activeCases;
  }

  @ApiModelProperty
  public int getMaxConcurrentCases() {
    return maxConcurrentCases;
  }

  public void setMaxConcurrentCases(int maxConcurrentCases) {
    this.maxConcurrentCases = maxConcurrentCases;
  }

  @ApiModelProperty
  public long getTotalMemoryUsed() {
    return totalMemoryUsed;
  }

  public void setTotalMemoryUsed(long totalMemoryUsed) {
    this.totalMemoryUsed = totalMemoryUsed;
  }

  @ApiModelProperty
  public long getMaxMemoryPerCase() {
    return maxMemoryPerCase;
  }

  public void setMaxMemoryPerCase(long maxMemoryPerCase) {
    this.maxMemoryPerCase = maxMemoryPerCase;
  }

  @ApiModelProperty
  public long getTotalItemsProcessed() {
    return totalItemsProcessed;
  }

  public void setTotalItemsProcessed(long totalItemsProcessed) {
    this.totalItemsProcessed = totalItemsProcessed;
  }
}
