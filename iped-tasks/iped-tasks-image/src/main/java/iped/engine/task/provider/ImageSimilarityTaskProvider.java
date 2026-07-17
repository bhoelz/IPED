package iped.engine.task.provider;

import iped.engine.task.AbstractTask;
import iped.engine.task.similarity.ImageSimilarityTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;
import java.util.List;

public class ImageSimilarityTaskProvider implements TaskProvider<AbstractTask> {

  private static final String TASK_ID = "iped.engine.task.similarity.ImageSimilarityTask";

  @Override
  public TaskDescriptor descriptor() {
    return TaskDescriptor.of(TASK_ID, dependencies());
  }

  @Override
  public AbstractTask createTask() {
    return new ImageSimilarityTask();
  }

  @Override
  public List<TaskDependency> dependencies() {
    return List.of(TaskDependency.after("iped.engine.task.ImageThumbTask"));
  }
}
