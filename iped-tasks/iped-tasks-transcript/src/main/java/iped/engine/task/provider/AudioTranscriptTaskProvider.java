package iped.engine.task.provider;

import iped.engine.task.AbstractTask;
import iped.engine.task.transcript.AudioTranscriptTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;
import java.util.List;

public class AudioTranscriptTaskProvider implements TaskProvider<AbstractTask> {

  private static final String TASK_ID = "iped.engine.task.transcript.AudioTranscriptTask";

  @Override
  public TaskDescriptor descriptor() {
    return TaskDescriptor.of(TASK_ID, dependencies());
  }

  @Override
  public AbstractTask createTask() {
    return new AudioTranscriptTask();
  }

  @Override
  public List<TaskDependency> dependencies() {
    // VideoThumbTask must run after transcription, see issue #100
    return List.of(
        TaskDependency.after("iped.engine.task.DuplicateTask"),
        TaskDependency.before("iped.engine.task.video.VideoThumbTask"));
  }
}
