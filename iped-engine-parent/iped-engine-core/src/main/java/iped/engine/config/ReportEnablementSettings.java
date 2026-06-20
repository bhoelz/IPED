package iped.engine.config;

/**
 * Minimal accessor {@code ExportFileTask} and {@code ThumbTask} (both in
 * {@code iped-tasks-forensics}) need from {@code HtmlReportTaskConfig}, owned
 * by {@code iped-tasks-report}.
 *
 * <p>A direct {@code iped-tasks-forensics} → {@code iped-tasks-report}
 * dependency would cycle: {@code iped-tasks-report} already depends on
 * {@code iped-tasks-image}, which depends on {@code iped-tasks-forensics}
 * (for {@code ThumbTask}). Looked up via {@link
 * ConfigurationManager#findObjectInstanceOf(Class)} instead — same pattern
 * used for {@link IndexSettings} and {@link ExportEnablementSettings}.
 */
public interface ReportEnablementSettings {

    boolean isEnabled();

}
