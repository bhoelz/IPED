package iped.engine.config;

import iped.engine.task.AbstractTask;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PluginTaskLoader {

  private static final List<String> PARENT_FIRST_PREFIXES =
      List.of(
          "java.",
          "javax.",
          "jdk.",
          "sun.",
          "iped.tasks.spi.",
          "iped.configuration.",
          "iped.engine.");

  TaskRegistry load(PluginConfig pluginConfig) {
    Map<String, TaskRegistry.TaskRegistration> registrations = new HashMap<>();
    List<String> skippedProviders = new ArrayList<>();
    List<String> loadedProviders = new ArrayList<>();

    loadClasspathProviders(registrations, loadedProviders, skippedProviders);

    for (File pluginCandidate : pluginConfig.getPluginJars()) {
      if (!pluginCandidate.getName().endsWith(".jar")) {
        continue;
      }

      try {
        URL[] urls = new URL[] {pluginCandidate.toURI().toURL()};
        ClassLoader parent = TaskProvider.class.getClassLoader();
        try (ChildFirstClassLoader classLoader =
            new ChildFirstClassLoader(urls, parent, PARENT_FIRST_PREFIXES)) {
          ServiceLoader<TaskProvider> loader = ServiceLoader.load(TaskProvider.class, classLoader);
          for (TaskProvider provider : loader) {
            registerProvider(registrations, loadedProviders, provider, pluginCandidate.getName());
          }
        }
      } catch (Exception e) {
        skippedProviders.add(pluginCandidate.getName() + ": " + e.getMessage());
        log.warn(
            "Failed to load task providers from plugin {}", pluginCandidate.getAbsolutePath(), e);
      }
    }

    if (!loadedProviders.isEmpty()) {
      log.info(
          "Loaded {} task providers from plugins: {}", loadedProviders.size(), loadedProviders);
    }
    if (!skippedProviders.isEmpty()) {
      log.warn("Skipped {} task providers/plugins: {}", skippedProviders.size(), skippedProviders);
    }

    return new TaskRegistry(registrations, loadedProviders, skippedProviders);
  }

  private void loadClasspathProviders(
      Map<String, TaskRegistry.TaskRegistration> registrations,
      List<String> loadedProviders,
      List<String> skippedProviders) {
    try {
      ServiceLoader<TaskProvider> loader = ServiceLoader.load(TaskProvider.class);
      for (TaskProvider provider : loader) {
        registerProvider(registrations, loadedProviders, provider, "classpath");
      }
    } catch (Exception e) {
      skippedProviders.add("classpath: " + e.getMessage());
      log.warn("Failed to load task providers from classpath", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void registerProvider(
      Map<String, TaskRegistry.TaskRegistration> registrations,
      List<String> loadedProviders,
      TaskProvider provider,
      String source)
      throws IOException {
    TaskDescriptor descriptor = provider.descriptor();
    String taskId = descriptor.id();

    TaskRegistry.TaskRegistration existing = registrations.get(taskId);
    if (existing != null) {
      // Plugin jars are also on the flat application classpath, so the same provider can be
      // discovered by both the classpath pass and the per-jar pass: not a conflict.
      if (provider.getClass().getName().equals(existing.providerClass)) {
        log.debug(
            "Provider {} for task '{}' already registered from {}, ignoring duplicate from {}",
            existing.providerClass,
            taskId,
            existing.source,
            source);
        return;
      }
      throw new IOException("Duplicate task id '" + taskId + "' provided by plugin " + source);
    }

    TaskRegistry.TaskFactory factory =
        () -> {
          Object task = provider.createTask();
          if (!(task instanceof AbstractTask)) {
            throw new IllegalStateException(
                "TaskProvider '"
                    + provider.getClass().getName()
                    + "' returned non-AbstractTask instance: "
                    + task.getClass().getName());
          }
          return (AbstractTask) task;
        };

    TaskRegistry.TaskRegistration registration =
        TaskRegistry.TaskRegistration.provider(
            taskId, descriptor, factory, source, provider.getClass().getName());
    registrations.put(taskId, registration);
    loadedProviders.add(provider.getClass().getName() + "@" + source);
  }
}
