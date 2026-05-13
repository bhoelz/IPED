package com.example.iped.tasks;

import java.util.List;

import iped.engine.task.AbstractTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;

public class ExampleTaskProvider implements TaskProvider<AbstractTask> {

    @Override
    public TaskDescriptor descriptor() {
        return new TaskDescriptor(
                "example-task",
                "Example Task",
                "enableExampleTask",
                List.of(
                        TaskDependency.after("iped.engine.task.HashTask"),
                        TaskDependency.optionalRequires("some-optional-task")));
    }

    @Override
    public AbstractTask createTask() {
        return new ExampleTask();
    }
}
