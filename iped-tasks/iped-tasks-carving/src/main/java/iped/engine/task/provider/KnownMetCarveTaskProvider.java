package iped.engine.task.provider;

import iped.engine.task.AbstractTask;
import iped.engine.task.carver.KnownMetCarveTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;

import java.util.List;

public class KnownMetCarveTaskProvider implements TaskProvider<AbstractTask> {

    private static final String TASK_ID = "iped.engine.task.carver.KnownMetCarveTask";

    @Override
    public TaskDescriptor descriptor() {
        return TaskDescriptor.of(TASK_ID, dependencies());
    }

    @Override
    public AbstractTask createTask() {
        return new KnownMetCarveTask();
    }

    @Override
    public List<TaskDependency> dependencies() {
        return List.of(TaskDependency.after("iped.engine.task.carver.CarverTask"));
    }
}
