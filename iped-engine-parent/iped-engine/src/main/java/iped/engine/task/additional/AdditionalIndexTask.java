package iped.engine.task.additional;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.datasource.IAdditionalDataSource;
import iped.engine.config.ConfigurationManager;
import iped.engine.task.AbstractTask;

import java.util.Collections;
import java.util.List;

/**
 * Terminal task in the additional-processing pipeline.
 *
 * <p>Instead of writing to the main Lucene index (as {@code IndexTask} does),
 * this task persists the item's current extra-attribute map to the
 * {@link IAdditionalDataSource} provided at construction time.</p>
 *
 * <p>It is always the <em>last</em> task in the additional pipeline:
 * {@code [userTask] → AdditionalIndexTask}.</p>
 */
public class AdditionalIndexTask extends AbstractTask {

    private final IAdditionalDataSource destination;
    private final String taskName;

    /**
     * @param destination the additional data source that will receive results
     * @param taskName    simple class name of the preceding user task
     *                    (used as the storage key)
     */
    public AdditionalIndexTask(IAdditionalDataSource destination, String taskName) {
        this.destination = destination;
        this.taskName    = taskName;
    }

    @Override
    public List<Configurable<?>> getConfigurables() {
        return Collections.emptyList();
    }

    @Override
    public void init(ConfigurationManager configurationManager) throws Exception {
        // nothing to initialise
    }

    @Override
    public void finish() throws Exception {
        // nothing to finalise
    }

    /**
     * Stores the item's current extra-attribute map under the (itemId, taskName)
     * key in the additional data source.
     *
     * @param evidence the item whose attributes will be persisted
     * @throws Exception on I/O failure
     */
    @Override
    protected void process(IItem evidence) throws Exception {
        destination.storeTaskResult(
                evidence.getId(),
                taskName,
                evidence.getExtraAttributeMap());
    }
}
