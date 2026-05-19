package iped.engine.task.provider;

import java.util.List;

import iped.engine.task.AbstractTask;
import iped.engine.task.ParsingTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;

public class ParsingTaskProvider implements TaskProvider<AbstractTask> {

    private static final String TASK_ID = "iped.engine.task.ParsingTask";

    @Override
    public TaskDescriptor descriptor() {
        return TaskDescriptor.of(TASK_ID, dependencies());
    }

    @Override
    public AbstractTask createTask() {
        return new ParsingTask();
    }

    @Override
    public List<TaskDependency> dependencies() {
        return List.of(TaskDependency.after("iped.engine.task.video.VideoThumbTask"));
    }
}
