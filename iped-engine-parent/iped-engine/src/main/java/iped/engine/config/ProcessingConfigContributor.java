package iped.engine.config;

import iped.configuration.Configurable;
import iped.engine.task.AbstractTask;

import java.io.IOException;

/**
 * Extends {@link EngineConfigContributor} with the task pipeline -- discovering
 * and instantiating every pipeline/plugin task and registering each task's own
 * {@link Configurable}s. Pulls in a dependency on every task/parser/carver
 * plugin module, so this must only be used by an actual case-processing entry
 * point ({@code iped-app}'s {@code Main}), never by something that merely opens
 * an already-processed case to read results.
 */
public class ProcessingConfigContributor implements ConfigContributor {

    public static final ProcessingConfigContributor INSTANCE = new ProcessingConfigContributor();

    @Override
    public void contribute(ConfigurationManager configManager) throws IOException {
        // OCRConfig/FileSystemConfig must be registered before the task-discovery loop
        // below: ParsingTaskConfig.processProperties() requires OCRConfig to already be
        // loaded, but ParsingTask.getConfigurables() lists itself before OCRConfig in its
        // own configurable list, so relying solely on that loop would load them too late.
        //
        // Calls the shared base directly rather than EngineConfigContributor.contribute():
        // that also registers a default IndexSettings fallback, which would otherwise win
        // over the real IndexTaskConfig the task loop below registers (findObjectInstanceOf
        // returns whichever IndexSettings was registered first).
        EngineConfigContributor.contributeBaseConfigs(configManager);

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
