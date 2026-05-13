package iped.tasks.spi;

import java.util.List;

public interface TaskProvider<T> {

    TaskDescriptor descriptor();

    T createTask();

    default List<TaskDependency> dependencies() {
        return descriptor().dependencies();
    }
}
