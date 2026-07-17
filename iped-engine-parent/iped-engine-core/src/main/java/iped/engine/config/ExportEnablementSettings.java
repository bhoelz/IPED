package iped.engine.config;

/**
 * Minimal accessor `Statistics` (iped-engine) needs from the automatic file-export configs ({@code
 * ExportByCategoriesConfig}, {@code ExportByKeywordsConfig}), both owned by {@code
 * iped-tasks-forensics}.
 *
 * <p>{@code iped-engine} can't depend on the concrete classes there without creating a cycle
 * ({@code iped-tasks-forensics} already depends on {@code iped-engine}), so it looks this up via
 * {@link ConfigurationManager#findObjectInstanceOf(Class)} instead — same pattern used for {@link
 * IndexSettings}.
 */
public interface ExportEnablementSettings {

  boolean isEnabled();
}
