package iped.tasks.spi;

public record TaskDependency(String taskId, TaskDependencyType type, boolean optional) {

  public TaskDependency {
    if (taskId == null || taskId.isBlank()) {
      throw new IllegalArgumentException("Dependency taskId must not be null or blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("Dependency type must not be null");
    }
  }

  public static TaskDependency requires(String taskId) {
    return new TaskDependency(taskId, TaskDependencyType.REQUIRES, false);
  }

  public static TaskDependency optionalRequires(String taskId) {
    return new TaskDependency(taskId, TaskDependencyType.REQUIRES, true);
  }

  public static TaskDependency before(String taskId) {
    return new TaskDependency(taskId, TaskDependencyType.BEFORE, false);
  }

  public static TaskDependency after(String taskId) {
    return new TaskDependency(taskId, TaskDependencyType.AFTER, false);
  }
}
