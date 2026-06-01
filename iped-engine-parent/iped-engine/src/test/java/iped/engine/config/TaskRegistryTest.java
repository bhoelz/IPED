package iped.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import iped.engine.task.AbstractTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;

class TaskRegistryTest {

    @Test
    void shouldOrderTasksUsingAfterDependencies() {
        Map<String, TaskRegistry.TaskRegistration> registrations = new LinkedHashMap<>();
        registrations.put("a", registration("a", List.of()));
        registrations.put("b", registration("b", List.of(TaskDependency.after("a"))));

        TaskRegistry registry = new TaskRegistry(registrations, List.of(), List.of());
        List<AbstractTask> ordered = registry.instantiateResolvedTasks();

        assertEquals("a", ordered.get(0).getName());
        assertEquals("b", ordered.get(1).getName());
    }

    @Test
    void shouldFailOnMissingRequiredDependency() {
        Map<String, TaskRegistry.TaskRegistration> registrations = new LinkedHashMap<>();
        registrations.put("a", registration("a", List.of(TaskDependency.requires("missing"))));

        TaskRegistry registry = new TaskRegistry(registrations, List.of(), List.of());

        assertThrows(IllegalStateException.class, registry::instantiateResolvedTasks);
    }

    @Test
    void shouldIgnoreMissingOptionalDependency() {
        Map<String, TaskRegistry.TaskRegistration> registrations = new LinkedHashMap<>();
        registrations.put("a", registration("a", List.of(TaskDependency.optionalRequires("missing"))));

        TaskRegistry registry = new TaskRegistry(registrations, List.of(), List.of());
        List<AbstractTask> ordered = registry.instantiateResolvedTasks();

        assertEquals(1, ordered.size());
        assertEquals("a", ordered.get(0).getName());
    }

    @Test
    void shouldFailOnDependencyCycle() {
        Map<String, TaskRegistry.TaskRegistration> registrations = new LinkedHashMap<>();
        registrations.put("a", registration("a", List.of(TaskDependency.after("b"))));
        registrations.put("b", registration("b", List.of(TaskDependency.after("a"))));

        TaskRegistry registry = new TaskRegistry(registrations, List.of(), List.of());

        assertThrows(IllegalStateException.class, registry::instantiateResolvedTasks);
    }

//    @Test
//    void shouldFailOnDuplicateIdsBetweenXmlAndPlugin() {
//        TaskRegistry pluginRegistry = new TaskRegistry(Map.of("a", registration("a", List.of())), List.of("x"), List.of());
//        List<TaskRegistry.TaskRegistration> xmlRegistrations = List.of(TaskRegistry.TaskRegistration.xmlTask("a", () -> new StubTask("a"), "xml"));
//
//        assertThrows(IllegalStateException.class, () -> TaskRegistry.merge(xmlRegistrations, pluginRegistry));
//    }

    private static TaskRegistry.TaskRegistration registration(String id, List<TaskDependency> dependencies) {
        TaskDescriptor descriptor = TaskDescriptor.of(id, dependencies);
        return TaskRegistry.TaskRegistration.provider(id, descriptor, () -> new StubTask(id), "test", "provider");
    }

    private static class StubTask extends AbstractTask {

        private final String name;

        StubTask(String name) {
            this.name = name;
        }

        @Override
        public List<iped.configuration.Configurable<?>> getConfigurables() {
            return List.of();
        }

        @Override
        public void init(ConfigurationManager configurationManager) {
        }

        @Override
        public void finish() {
        }

        @Override
        protected void process(iped.data.IItem evidence) {
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
