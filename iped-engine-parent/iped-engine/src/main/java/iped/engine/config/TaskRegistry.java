package iped.engine.config;

import iped.engine.task.AbstractTask;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDependencyType;
import iped.tasks.spi.TaskDescriptor;

import java.util.*;
import java.util.stream.Collectors;

class TaskRegistry {

    @FunctionalInterface
    interface TaskFactory {
        AbstractTask create();
    }

    static final class TaskRegistration {
        final String id;
        final TaskDescriptor descriptor;
        final TaskFactory factory;
        final String source;
        final String providerClass;
        final boolean pluginProvider;

        private TaskRegistration(String id, TaskDescriptor descriptor, TaskFactory factory, String source, String providerClass, boolean pluginProvider) {
            this.id = id;
            this.descriptor = descriptor;
            this.factory = factory;
            this.source = source;
            this.providerClass = providerClass;
            this.pluginProvider = pluginProvider;
        }

        static TaskRegistration pipelineTask(String id, TaskFactory factory, String source) {
            return new TaskRegistration(id, TaskDescriptor.of(id), factory, source, null, false);
        }

        static TaskRegistration provider(String id, TaskDescriptor descriptor, TaskFactory factory, String source, String providerClass) {
            return new TaskRegistration(id, descriptor, factory, source, providerClass, true);
        }
    }

    private final Map<String, TaskRegistration> registrations;
    private final List<String> loadedProviders;
    private final List<String> skippedProviders;

    TaskRegistry(Map<String, TaskRegistration> registrations, List<String> loadedProviders, List<String> skippedProviders) {
        this.registrations = new LinkedHashMap<>(registrations);
        this.loadedProviders = List.copyOf(loadedProviders);
        this.skippedProviders = List.copyOf(skippedProviders);
    }

    Map<String, TaskRegistration> registrations() {
        return Collections.unmodifiableMap(registrations);
    }

    List<String> loadedProviders() {
        return loadedProviders;
    }

    List<String> skippedProviders() {
        return skippedProviders;
    }

    List<AbstractTask> instantiateResolvedTasks() {
        List<TaskRegistration> resolved = resolveOrder();
        List<AbstractTask> tasks = new ArrayList<>(resolved.size());
        for (TaskRegistration registration : resolved) {
            tasks.add(registration.factory.create());
        }
        return tasks;
    }

    private List<TaskRegistration> resolveOrder() {
        Map<String, Set<String>> adjacency = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        // Most pipeline tasks declare no explicit dependency metadata at all, so the
        // topological sort below must fall back to original declaration order among
        // tasks with no ordering constraint between them - that's the only thing that
        // makes TaskInstaller.toml's "order is very sensitive" guarantee actually hold.
        Map<String, Integer> declarationOrder = new HashMap<>();

        int i = 0;
        for (String id : registrations.keySet()) {
            adjacency.put(id, new java.util.LinkedHashSet<>());
            indegree.put(id, 0);
            declarationOrder.put(id, i++);
        }

        for (TaskRegistration registration : registrations.values()) {
            for (TaskDependency dep : registration.descriptor.dependencies()) {
                String dependencyTaskId = dep.taskId();
                if (!registrations.containsKey(dependencyTaskId)) {
                    // AFTER/BEFORE are ordering hints: silently skip if the target task is absent.
                    // Only REQUIRES is a hard error when not satisfied.
                    if (dep.optional() || dep.type() != TaskDependencyType.REQUIRES) {
                        continue;
                    }
                    throw new IllegalStateException("Task '" + registration.id + "' has missing dependency '" + dependencyTaskId + "'");
                }

                if (dep.type() == TaskDependencyType.REQUIRES || dep.type() == TaskDependencyType.AFTER) {
                    addEdge(adjacency, indegree, dependencyTaskId, registration.id);
                } else if (dep.type() == TaskDependencyType.BEFORE) {
                    addEdge(adjacency, indegree, registration.id, dependencyTaskId);
                }
            }
        }

        Queue<String> queue = new PriorityQueue<>(Comparator.comparingInt(declarationOrder::get));
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<TaskRegistration> ordered = new ArrayList<>(registrations.size());
        while (!queue.isEmpty()) {
            String current = queue.remove();
            ordered.add(registrations.get(current));
            for (String next : adjacency.get(current)) {
                int degree = indegree.merge(next, -1, Integer::sum);
                if (degree == 0) {
                    queue.add(next);
                }
            }
        }

        if (ordered.size() != registrations.size()) {
            List<String> cyclicNodes = indegree.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            throw new IllegalStateException("Task dependency cycle detected: " + cyclicNodes);
        }

        return ordered;
    }

    private static void addEdge(Map<String, Set<String>> adjacency, Map<String, Integer> indegree, String from, String to) {
        if (adjacency.get(from).add(to)) {
            indegree.merge(to, 1, Integer::sum);
        }
    }

    static TaskRegistry merge(Collection<TaskRegistration> pipelineRegistrations, TaskRegistry pluginRegistry) {
        Map<String, TaskRegistration> all = new LinkedHashMap<>();
        for (TaskRegistration pipelineRegistration : pipelineRegistrations) {
            all.put(pipelineRegistration.id, pipelineRegistration);
        }
        for (TaskRegistration pluginRegistration : pluginRegistry.registrations.values()) {
            if (all.containsKey(pluginRegistration.id)) {
                all.put(pluginRegistration.id, pluginRegistration);
                continue;
            }
            all.put(pluginRegistration.id, pluginRegistration);
        }
        return new TaskRegistry(all, pluginRegistry.loadedProviders, pluginRegistry.skippedProviders);
    }
}
