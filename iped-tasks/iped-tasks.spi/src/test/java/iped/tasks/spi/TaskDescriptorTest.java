package iped.tasks.spi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskDescriptorTest {

    @Test
    public void ofCreatesMinimalDescriptor() {
        TaskDescriptor d = TaskDescriptor.of("hash");
        assertEquals("hash", d.id());
        assertEquals("hash", d.displayName());
        assertNull(d.enableProperty());
        assertTrue(d.dependencies().isEmpty());
    }

    @Test
    public void rejectsNullOrBlankId() {
        assertThrows(IllegalArgumentException.class, () -> TaskDescriptor.of(null));
        assertThrows(IllegalArgumentException.class, () -> TaskDescriptor.of("  "));
    }

    @Test
    public void blankDisplayNameFallsBackToId() {
        TaskDescriptor d = new TaskDescriptor("hash", "  ", null, null);
        assertEquals("hash", d.displayName());
    }

    @Test
    public void nullDependenciesBecomeEmptyList() {
        TaskDescriptor d = new TaskDescriptor("hash", "Hash", null, null);
        assertTrue(d.dependencies().isEmpty());
    }

    @Test
    public void dependenciesAreCopiedAndImmutable() {
        List<TaskDependency> deps = new ArrayList<>();
        deps.add(TaskDependency.requires("a"));
        TaskDescriptor d = TaskDescriptor.of("hash", deps);

        deps.add(TaskDependency.requires("b"));
        assertEquals(1, d.dependencies().size(), "descriptor must not see later mutations");
        assertThrows(UnsupportedOperationException.class, () -> d.dependencies().add(TaskDependency.requires("c")));
    }

    @Test
    public void withersReturnModifiedCopies() {
        TaskDescriptor base = TaskDescriptor.of("hash");

        TaskDescriptor named = base.withDisplayName("Hashing");
        assertEquals("Hashing", named.displayName());
        assertEquals("hash", base.displayName(), "original is unchanged");

        TaskDescriptor enabled = base.withEnableProperty("enableHash");
        assertEquals("enableHash", enabled.enableProperty());

        TaskDescriptor withDeps = base.withDependencies(List.of(TaskDependency.after("sig")));
        assertEquals(1, withDeps.dependencies().size());
    }

    @Test
    public void withDisplayNameNullFallsBackToId() {
        TaskDescriptor d = TaskDescriptor.of("hash").withDisplayName(null);
        assertEquals("hash", d.displayName());
    }

    @Test
    public void providerDependenciesDefaultToDescriptor() {
        List<TaskDependency> deps = List.of(TaskDependency.requires("a"));
        TaskProvider<String> provider = new TaskProvider<>() {
            @Override
            public TaskDescriptor descriptor() {
                return TaskDescriptor.of("test", deps);
            }

            @Override
            public String createTask() {
                return "task";
            }
        };

        assertEquals(deps, provider.dependencies());
        assertEquals("task", provider.createTask());
    }
}
