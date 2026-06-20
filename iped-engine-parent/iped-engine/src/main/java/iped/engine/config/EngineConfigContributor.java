package iped.engine.config;

import iped.configuration.Configurable;
import iped.engine.task.AbstractTask;

import java.io.IOException;

/**
 * Registers the {@link Configurable}s that {@code iped-engine-core}'s
 * {@link Configuration} cannot construct directly, because their implementation
 * classes live here in {@code iped-engine} (which depends on {@code iped-engine-core},
 * not the other way around).
 *
 * <p>Pass {@link #INSTANCE} as the {@code extraConfigs} argument to
 * {@link Configuration#loadConfigurables(String, boolean, ConfigContributor)} from
 * any caller that needs the full engine config set (i.e. anywhere {@code loadAll}
 * is {@code true}: actual case processing, the web API, and {@link iped.engine.data.IPEDSource}).
 */
public class EngineConfigContributor implements ConfigContributor {

    public static final EngineConfigContributor INSTANCE = new EngineConfigContributor();

    @Override
    public void contribute(ConfigurationManager configManager) throws IOException {
        // OCRConfig/FileSystemConfig must be registered before the task-discovery loop
        // below: ParsingTaskConfig.processProperties() requires OCRConfig to already be
        // loaded, but ParsingTask.getConfigurables() lists itself before OCRConfig in its
        // own configurable list, so relying solely on that loop would load them too late.
        configManager.addObject(new OCRConfig());
        configManager.addObject(new FileSystemConfig());
        configManager.addObject(new AnalysisConfig());
        configManager.addObject(new AIFiltersConfig());
        configManager.addObject(new ProcessingPriorityConfig());

        configManager.addObject(new EnableTaskProperty(FaceRecognitionConfig.enableParam));
        configManager.addObject(new EnableTaskProperty(AgeEstimationConfig.enableParam));

        TaskInstallerConfig taskConfig = new TaskInstallerConfig();
        configManager.addObject(taskConfig);

        // must load taskConfig before using it
        configManager.loadConfig(taskConfig);

        for (AbstractTask task : taskConfig.getNewTaskInstances()) {
            for (Configurable<?> configurable : task.getConfigurables()) {
                configManager.addObject(configurable);
            }
        }
    }
}
