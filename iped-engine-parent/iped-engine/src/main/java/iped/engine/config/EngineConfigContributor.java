package iped.engine.config;

import java.io.IOException;

/**
 * Registers the {@link iped.configuration.Configurable}s that {@code iped-engine-core}'s {@link
 * Configuration} cannot construct directly, because their implementation classes live here in
 * {@code iped-engine} (which depends on {@code iped-engine-core}, not the other way around).
 *
 * <p>This covers only the configs needed to open and read an already-processed case (search,
 * browse, view results) -- none of it touches the task pipeline, so callers that just need read
 * access to results pull in no dependency on any task/parser/carver plugin module. Pass {@link
 * #INSTANCE} as the {@code extraConfigs} argument to {@link Configuration#loadConfigurables(String,
 * boolean, ConfigContributor)} from any such caller: {@link iped.engine.data.IPEDSource}, the web
 * API.
 *
 * <p>Actual case processing needs more than this -- see {@link ProcessingConfigContributor}, which
 * extends this with the task pipeline. {@code iped-app}'s {@code Main} (the only real processing
 * entry point) is the only caller that should use that one instead.
 */
public class EngineConfigContributor implements ConfigContributor {

  public static final EngineConfigContributor INSTANCE = new EngineConfigContributor();

  @Override
  public void contribute(ConfigurationManager configManager) throws IOException {
    contributeBaseConfigs(configManager);

    // The real IndexSettings impl (IndexTaskConfig) is only registered by the
    // task pipeline (see ProcessingConfigContributor), which read-only callers
    // never run. Without this fallback, AppAnalyzer's
    // findObjectInstanceOf(IndexSettings.class) lookup returns null and NPEs.
    configManager.addObject(DefaultIndexSettings.INSTANCE);

    // Same situation for CategoryConfig: normally registered only by
    // SetCategoryTask.getConfigurables() in the task loop. Unlike IndexSettings
    // there's no plugin-module dependency issue here -- CategoryConfig lives in
    // iped-engine itself -- so this is just a plain extra registration, not a
    // fallback that risks shadowing a "real" one (this contributor never runs
    // alongside the task loop; see ProcessingConfigContributor).
    configManager.addObject(new CategoryConfig());
  }

  /**
   * Registers the configs shared by both the read-only path (this class) and the processing path
   * ({@link ProcessingConfigContributor}). Package-private: {@link ProcessingConfigContributor}
   * calls this directly instead of {@link #contribute}, so it never registers {@link
   * DefaultIndexSettings} -- its task loop registers the real {@code IndexTaskConfig} instead, and
   * {@code findObjectInstanceOf} would otherwise return whichever IndexSettings was registered
   * first rather than the real one.
   */
  static void contributeBaseConfigs(ConfigurationManager configManager) throws IOException {
    configManager.addObject(new OCRConfig());
    configManager.addObject(new FileSystemConfig());
    configManager.addObject(new AnalysisConfig());
    configManager.addObject(new AIFiltersConfig());
    configManager.addObject(new ProcessingPriorityConfig());

    configManager.addObject(new EnableTaskProperty(FaceRecognitionConfig.enableParam));
    configManager.addObject(new EnableTaskProperty(AgeEstimationConfig.enableParam));
  }
}
