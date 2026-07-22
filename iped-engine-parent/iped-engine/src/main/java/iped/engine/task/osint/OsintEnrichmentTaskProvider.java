package iped.engine.task.osint;

import iped.osint.spi.OsintExecutionMode;
import iped.tasks.spi.TaskDependency;
import iped.tasks.spi.TaskDescriptor;
import iped.tasks.spi.TaskProvider;
import iped.tasks.spi.TaskStateModel;

import java.util.List;

public class OsintEnrichmentTaskProvider implements TaskProvider<OsintEnrichmentTask> {

    @Override
    public TaskDescriptor descriptor() {
        return new TaskDescriptor(
                "iped.engine.task.osint.OsintEnrichmentTask",
                "OSINT Enrichment Task",
                null,
                List.of(
                        TaskDependency.after("iped.engine.task.regex.RegexTask"),
                        TaskDependency.after("iped.engine.task.NamedEntityTask")));
    }

    @Override
    public OsintEnrichmentTask createTask() {
        return new OsintEnrichmentTask();
    }

    @Override
    public TaskStateModel stateModel() {
        return TaskStateModel.CASE_SCOPED;
    }
}
