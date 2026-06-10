package iped.distributed.agent;

import iped.distributed.config.DistributedConfig;
import iped.distributed.coordinator.CoordinatorClient;
import iped.engine.config.ConfigurationDirectory;
import iped.engine.config.ConfigurationManager;

import lombok.extern.slf4j.Slf4j;
/**
 * Command-line entry point for a Task Agent process.
 *
 * <p>Each worker machine runs one JVM per task type. This launcher:
 * <ol>
 *   <li>Parses CLI arguments</li>
 *   <li>Loads IPED configuration (so tasks can access FileSystemConfig, etc.)</li>
 *   <li>Registers the known {@link InputStreamFactoryRegistry} builders</li>
 *   <li>Resolves the pipeline stage from the Coordinator</li>
 *   <li>Instantiates the configured {@code AbstractTask} via reflection</li>
 *   <li>Wraps it in a {@link TaskAgent} and starts it</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>
 * java -cp iped-distributed.jar iped.distributed.agent.TaskAgentLauncher \
 *      --caseId    my-case-id               \
 *      --taskType  HashTask                 \
 *      --taskClass iped.engine.task.HashTask \
 *      --kafka     broker1:9092             \
 *      --coordinator http://coord:8484      \
 *      --config    /etc/iped               \
 *      --parallelism 8
 * </pre>
 */
@Slf4j
public class TaskAgentLauncher {


    public static void main(String[] args) throws Exception {
        TaskAgentConfig cfg = parseArgs(args);
        if (cfg == null) { printUsage(); System.exit(1); }

        log.info("Starting Task Agent: caseId={}, taskType={}, parallelism={}",
                cfg.caseId, cfg.taskType, cfg.parallelism);

        // 1. Load IPED configuration
        if (cfg.configPath != null) {
            ConfigurationDirectory dir = new ConfigurationDirectory(java.nio.file.Paths.get(cfg.configPath));
            ConfigurationManager cm = ConfigurationManager.createInstance(dir);
            cm.loadConfigs();
        }

        // 2. Register known InputStreamFactory builders
        //    (additional builders can be added here for other datasource types)
        registerBuiltinFactories(cfg.sharedStorageRoot);

        // 3. Build DistributedConfig from the agent config
        DistributedConfig distCfg = buildDistributedConfig(cfg);

        // 4. Resolve stage number from Coordinator
        CoordinatorClient coordinator = new CoordinatorClient(cfg.coordinatorUrl);
        int stageNumber = coordinator.getStageForTask(cfg.caseId, cfg.taskType);
        log.info("Task '{}' is at pipeline stage {}", cfg.taskType, stageNumber);

        // 5. Instantiate the task via reflection — task code is completely unchanged
        Object taskInstance = Class.forName(cfg.taskClass).getDeclaredConstructor().newInstance();
        if (!(taskInstance instanceof TaskAgent.TaskProcessor)) {
            // Wrap the task in an adapter that calls process(IItem) on the AbstractTask
            TaskAgent.TaskProcessor processor = buildTaskProcessor(taskInstance, cfg.taskClass);

            // 6. Start the agent
            try (TaskAgent agent = new TaskAgent(
                    cfg.caseId, cfg.taskType, stageNumber, processor, distCfg, coordinator)) {
                Runtime.getRuntime().addShutdownHook(
                        new Thread(agent::stop, "agent-shutdown-" + cfg.taskType));
                agent.start(); // blocks until stopped
            }
        } else {
            // Task already implements TaskProcessor (unusual but supported)
            try (TaskAgent agent = new TaskAgent(
                    cfg.caseId, cfg.taskType, stageNumber,
                    (TaskAgent.TaskProcessor) taskInstance, distCfg, coordinator)) {
                Runtime.getRuntime().addShutdownHook(
                        new Thread(agent::stop, "agent-shutdown-" + cfg.taskType));
                agent.start();
            }
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Builds a {@link TaskAgent.TaskProcessor} that delegates to an {@code AbstractTask}
     * via reflection, keeping the task code completely independent of this class.
     */
    private static TaskAgent.TaskProcessor buildTaskProcessor(Object task, String taskClass) {
        return item -> {
            try {
                // Calls AbstractTask.process(IItem) reflectively
                java.lang.reflect.Method processMethod =
                        task.getClass().getMethod("process", iped.data.IItem.class);
                processMethod.invoke(task, item);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                throw new RuntimeException("Task execution failed", cause);
            }
        };
    }

    private static void registerBuiltinFactories(String sharedStorageRoot) {
        // Sleuthkit
        try {
            Class.forName("iped.engine.sleuthkit.SleuthkitInputStreamFactory");
            InputStreamFactoryRegistry.register(
                "iped.engine.sleuthkit.SleuthkitInputStreamFactory",
                params -> {
                    try {
                        java.lang.Class<?> cls = Class.forName(
                            "iped.engine.sleuthkit.SleuthkitInputStreamFactory");
                        java.lang.reflect.Constructor<?> ctor =
                            cls.getConstructor(java.nio.file.Path.class, long.class);
                        return (iped.io.ISeekableInputStreamFactory) ctor.newInstance(
                            java.nio.file.Paths.get(params.get("dbPath")),
                            Long.parseLong(params.get("objectId")));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to reconstruct SleuthkitInputStreamFactory", e);
                    }
                }
            );
        } catch (ClassNotFoundException ignored) {
            // iped-sleuthkit not on classpath — skip
        }

        // AD1
        try {
            Class.forName("iped.engine.datasource.AD1DataSourceReader$AD1InputStreamFactory");
            InputStreamFactoryRegistry.register(
                "iped.engine.datasource.AD1DataSourceReader$AD1InputStreamFactory",
                params -> {
                    try {
                        java.lang.Class<?> cls = Class.forName(
                            "iped.engine.datasource.AD1DataSourceReader$AD1InputStreamFactory");
                        java.lang.reflect.Constructor<?> ctor =
                            cls.getConstructor(java.nio.file.Path.class);
                        return (iped.io.ISeekableInputStreamFactory) ctor.newInstance(
                            java.nio.file.Paths.get(params.get("datasourcePath")));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to reconstruct AD1InputStreamFactory", e);
                    }
                }
            );
        } catch (ClassNotFoundException ignored) {}

        // UFDR
        try {
            Class.forName("iped.engine.io.UFDRInputStreamFactory");
            InputStreamFactoryRegistry.register(
                "iped.engine.io.UFDRInputStreamFactory",
                params -> {
                    try {
                        java.lang.Class<?> cls = Class.forName("iped.engine.io.UFDRInputStreamFactory");
                        java.lang.reflect.Constructor<?> ctor =
                            cls.getConstructor(java.nio.file.Path.class);
                        return (iped.io.ISeekableInputStreamFactory) ctor.newInstance(
                            java.nio.file.Paths.get(params.get("ufdrPath")));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to reconstruct UFDRInputStreamFactory", e);
                    }
                }
            );
        } catch (ClassNotFoundException ignored) {}

        log.info("Built-in InputStreamFactory builders registered");
    }

    private static DistributedConfig buildDistributedConfig(TaskAgentConfig cfg) {
        DistributedConfig dist = new DistributedConfig();
        // Use reflection-free setters via a properties object
        iped.utils.UTF8Properties props = new iped.utils.UTF8Properties();
        props.setProperty("enableDistributed",        "true");
        props.setProperty("kafkaBootstrapServers",    cfg.kafkaBootstrapServers);
        props.setProperty("coordinatorServerUrl",     cfg.coordinatorUrl);
        props.setProperty("sharedStorageRoot",        cfg.sharedStorageRoot);
        props.setProperty("agentParallelism",         String.valueOf(cfg.parallelism));
        props.setProperty("itemTimeoutSeconds",       String.valueOf(cfg.itemTimeoutSeconds));
        props.setProperty("deadLetterTopicSuffix",    cfg.deadLetterTopicSuffix);
        props.setProperty("exactlyOnce",              String.valueOf(cfg.exactlyOnce));
        dist.processProperties(props);
        return dist;
    }

    // -----------------------------------------------------------------------

    private static TaskAgentConfig parseArgs(String[] args) {
        if (args == null || args.length == 0) return null;
        TaskAgentConfig cfg = new TaskAgentConfig();
        for (int i = 0; i < args.length - 1; i += 2) {
            switch (args[i]) {
                case "--caseId":       cfg.caseId       = args[i+1]; break;
                case "--taskType":     cfg.taskType     = args[i+1]; break;
                case "--taskClass":    cfg.taskClass    = args[i+1]; break;
                case "--kafka":        cfg.kafkaBootstrapServers = args[i+1]; break;
                case "--coordinator":  cfg.coordinatorUrl = args[i+1]; break;
                case "--config":       cfg.configPath   = args[i+1]; break;
                case "--parallelism":  cfg.parallelism  = Integer.parseInt(args[i+1]); break;
                case "--sharedStorage": cfg.sharedStorageRoot = args[i+1]; break;
                default: log.warn("Unknown argument: {}", args[i]);
            }
        }
        if (cfg.caseId == null || cfg.taskType == null || cfg.taskClass == null) return null;
        return cfg;
    }

    private static void printUsage() {
        System.err.println("Usage: TaskAgentLauncher");
        System.err.println("  --caseId      <case-id>          (required)");
        System.err.println("  --taskType    <TaskSimpleName>   (required)");
        System.err.println("  --taskClass   <FQCN>            (required)");
        System.err.println("  --kafka       <bootstrap>        (default: localhost:9092)");
        System.err.println("  --coordinator <url>              (default: http://localhost:8484)");
        System.err.println("  --config      <path>             (optional config dir)");
        System.err.println("  --parallelism <N>                (default: 4)");
        System.err.println("  --sharedStorage <path>           (default: /mnt/iped-shared)");
    }
}
