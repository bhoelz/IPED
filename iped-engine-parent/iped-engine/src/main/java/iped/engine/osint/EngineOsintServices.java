package iped.engine.osint;

import iped.data.IIPEDSource;
import iped.engine.config.Configuration;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.PluginConfig;
import iped.engine.data.IPEDSource;
import iped.osint.core.BasicOsintIndicatorExtractor;
import iped.osint.core.OsintAnalysisRunner;
import iped.osint.core.OsintIndicatorExtractor;
import iped.osint.core.OsintPluginsConfig;
import iped.osint.core.OsintService;
import iped.osint.core.ServiceLoaderOsintPluginRegistry;
import iped.osint.store.FileOsintResultStore;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EngineOsintServices {

    private static final Map<File, OsintService> SERVICES = new ConcurrentHashMap<>();
    private static final OsintIndicatorExtractor EXTRACTOR = new BasicOsintIndicatorExtractor();

    private EngineOsintServices() {
    }

    public static OsintService forSource(IIPEDSource source) throws IOException {
        return SERVICES.computeIfAbsent(source.getCaseDir(), ignored -> createService(source));
    }

    public static OsintAnalysisRunner analysisRunner(IIPEDSource source) throws IOException {
        return new OsintAnalysisRunner(source, forSource(source), EXTRACTOR);
    }

    public static OsintIndicatorExtractor extractor() {
        return EXTRACTOR;
    }

    public static EngineOsintScriptingFacade scriptingFacade(File moduleDir) {
        return new EngineOsintScriptingFacade(moduleDir);
    }

    private static OsintService createService(IIPEDSource source) {
        try {
            Configuration configuration = Configuration.getInstance();
            PluginConfig pluginConfig = ConfigurationManager.get().findObject(PluginConfig.class);
            File[] pluginJars = pluginConfig == null ? new File[0] : pluginConfig.getPluginJars();
            OsintPluginsConfig osintConfig = OsintPluginsConfig.load(
                    configuration.configPath == null ? null : new File(configuration.configPath),
                    configuration.appRoot == null ? source.getModuleDir() : new File(configuration.appRoot));
            return new OsintService(
                    source.getCaseDir().getName(),
                    ServiceLoaderOsintPluginRegistry.load(pluginJars),
                    new FileOsintResultStore(source.getModuleDir().toPath().resolve(".osint-store")),
                    osintConfig,
                    Clock.systemUTC());
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize OSINT services for " + source.getCaseDir(), e);
        }
    }

    public static IPEDSource openReadOnlySource(File moduleDir) {
        return new IPEDSource(moduleDir.getParentFile());
    }
}
