package iped.engine.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iped.configuration.Configurable;
import iped.engine.task.AbstractTask;
import iped.exception.IPEDException;
import iped.utils.TomlProperties;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskInstallerConfig implements Configurable<String> {

  private static final long serialVersionUID = 1L;
  private static final String CONFIG_FILE = "TaskInstaller.toml"; // $NON-NLS-1$
  private static final String TASKS_KEY = "tasks"; // $NON-NLS-1$
  public static final String SCRIPT_BASE = "scripts/tasks"; // $NON-NLS-1$

  private final List<String> tomlContents = new ArrayList<>();

  /**
   * Not configuration data: resolves and instantiates the entire task pipeline from scratch on
   * every call. Excluded from JSON serialization (schema validation, the config-editor REST API,
   * etc.) so those generic, bean-style tools don't trigger an expensive, non-idempotent task-graph
   * rebuild just by looking at this object.
   */
  @JsonIgnore
  public List<AbstractTask> getNewTaskInstances() {
    Map<String, TaskRegistry.TaskRegistration> pipelineTasks = new LinkedHashMap<>();
    try {
      loadPipelineTasks(pipelineTasks);
    } catch (Exception e) {
      throw new RuntimeException("Error loading task pipeline from " + CONFIG_FILE, e);
    }

    PluginConfig pluginConfig = ConfigurationManager.get().findObject(PluginConfig.class);
    TaskRegistry pluginRegistry =
        pluginConfig == null
            ? new TaskRegistry(Map.of(), List.of(), List.of())
            : new PluginTaskLoader().load(pluginConfig);
    TaskRegistry registry = TaskRegistry.merge(pipelineTasks.values(), pluginRegistry);

    if (!pluginRegistry.loadedProviders().isEmpty()) {
      log.info(
          "Resolving task graph with {} pipeline tasks and {} plugin tasks",
          pipelineTasks.size(),
          pluginRegistry.registrations().size());
    }

    List<AbstractTask> tasks = registry.instantiateResolvedTasks();
    log.info("Resolved {} total tasks for execution pipeline", tasks.size());
    return tasks;
  }

  @Override
  public Filter<Path> getResourceLookupFilter() {
    return entry -> entry.endsWith(CONFIG_FILE);
  }

  @Override
  public void processConfig(Path resource) throws IOException {
    byte[] bytes = Files.readAllBytes(resource);
    // Each discovered config file replaces the previous one so that a profile's
    // TaskInstaller.toml fully controls the task list instead of merging with the
    // base config (same override semantics used by all other Configurable types).
    tomlContents.clear();
    tomlContents.add(new String(bytes, StandardCharsets.UTF_8));
  }

  /**
   * The pipeline is an ordered list of entries under the "tasks" key. Order is preserved: it
   * defines the task installation order, which directly impacts processing correctness. Entries
   * ending in ".js" or ".py" are scripts from the scripts/tasks folder; any other entry is a task
   * class name.
   */
  private void loadPipelineTasks(Map<String, TaskRegistry.TaskRegistration> tasks)
      throws IOException {
    for (String toml : tomlContents) {
      TomlProperties properties = new TomlProperties();
      properties.load(new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8)));
      for (String entry : properties.getListProperty(TASKS_KEY)) {
        if (isScript(entry)) {
          File script = locateScript(entry);
          tasks.putIfAbsent(
              entry,
              TaskRegistry.TaskRegistration.pipelineTask(
                  entry, () -> getScriptTask(script), CONFIG_FILE));
        } else {
          tasks.putIfAbsent(
              entry,
              TaskRegistry.TaskRegistration.pipelineTask(
                  entry, () -> instantiateClassTask(entry), CONFIG_FILE));
        }
      }
    }
  }

  private static boolean isScript(String entry) {
    return entry.endsWith(".js") || entry.endsWith(".py"); // $NON-NLS-1$ //$NON-NLS-2$
  }

  private AbstractTask instantiateClassTask(String className) {
    try {
      return (AbstractTask) Class.forName(className).getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Could not instantiate pipeline task class " + className, e);
    }
  }

  private File locateScript(String scriptName) {
    File scriptDir = new File(Configuration.getInstance().configPath, SCRIPT_BASE);
    File script = new File(scriptDir, scriptName);
    if (!script.exists()) {
      scriptDir = new File(Configuration.getInstance().appRoot, SCRIPT_BASE);
      script = new File(scriptDir, scriptName);
      if (!script.exists()) {
        throw new IPEDException(
            "Script File not found: " + script.getAbsolutePath()); // $NON-NLS-1$
      }
    }
    return script;
  }

  private AbstractTask getScriptTask(File script) {
    String className =
        script.getName().endsWith(".py")
            ? "iped.engine.task.PythonTask"
            : "iped.engine.task.ScriptTask";
    try {
      return (AbstractTask)
          Class.forName(className).getDeclaredConstructor(File.class).newInstance(script);
    } catch (Exception e) {
      throw new RuntimeException("Could not instantiate script task class " + className, e);
    }
  }

  @Override
  public String getConfiguration() {
    return String.join(System.lineSeparator(), tomlContents);
  }

  @Override
  public void setConfiguration(String config) {
    tomlContents.clear();
    if (config != null && !config.isBlank()) {
      tomlContents.add(config);
    }
  }
}
