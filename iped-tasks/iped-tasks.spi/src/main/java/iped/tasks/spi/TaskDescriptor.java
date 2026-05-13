package iped.tasks.spi;

import java.util.List;
import java.util.Objects;

public record TaskDescriptor(String id, String displayName, String enableProperty, List<TaskDependency> dependencies) {

    public TaskDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Task id must not be null or blank");
        }
        displayName = (displayName == null || displayName.isBlank()) ? id : displayName;
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    public static TaskDescriptor of(String id) {
        return new TaskDescriptor(id, id, null, List.of());
    }

    public static TaskDescriptor of(String id, List<TaskDependency> dependencies) {
        return new TaskDescriptor(id, id, null, dependencies);
    }

    public TaskDescriptor withDependencies(List<TaskDependency> deps) {
        return new TaskDescriptor(id, displayName, enableProperty, deps);
    }

    public TaskDescriptor withEnableProperty(String property) {
        return new TaskDescriptor(id, displayName, property, dependencies);
    }

    public TaskDescriptor withDisplayName(String name) {
        return new TaskDescriptor(id, Objects.requireNonNullElse(name, id), enableProperty, dependencies);
    }
}
