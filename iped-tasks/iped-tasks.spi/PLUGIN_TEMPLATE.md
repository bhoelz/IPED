# Task Plugin Template (`iped-tasks.spi`)

Each plugin JAR must provide at least:

1. A `TaskProvider` implementation class.
2. A service registration file:
`META-INF/services/iped.tasks.spi.TaskProvider`
3. Optional configuration files consumed by the task.

## Provider skeleton

```java
package com.example.iped.plugin;

import iped.engine.task.AbstractTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;

public class ExampleTaskProvider implements TaskProvider<AbstractTask> {

    @Override
    public TaskDescriptor descriptor() {
        return TaskDescriptor.of(
            "example-task",
            java.util.List.of(TaskDependency.after("iped.engine.task.HashTask"))
        );
    }

    @Override
    public AbstractTask createTask() {
        return new ExampleTask();
    }
}
```

## Service file content

File: `META-INF/services/iped.tasks.spi.TaskProvider`

```text
com.example.iped.plugin.ExampleTaskProvider
```
