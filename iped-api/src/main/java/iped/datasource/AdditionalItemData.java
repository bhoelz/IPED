package iped.datasource;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Holds the result of one additional-processing task run on a specific item.
 * Instances are immutable.
 *
 * @param taskName        simple class name of the task that produced these
 *                        results
 * @param extraAttributes extra attributes written by the task
 * @param processedAt     when the task was executed
 */
public record AdditionalItemData(String taskName, Map<String, Object> extraAttributes, Instant processedAt) {

    /**
     * Canonical constructor that wraps {@code extraAttributes} in an unmodifiable
     * view.
     *
     * @param taskName        simple class name of the task that produced these
     *                        results
     * @param extraAttributes extra attributes written by the task
     * @param processedAt     when the task was executed
     */
    public AdditionalItemData(String taskName, Map<String, Object> extraAttributes, Instant processedAt) {
        this.taskName = taskName;
        this.extraAttributes = Collections.unmodifiableMap(extraAttributes);
        this.processedAt = processedAt;
    }

    /**
     * @return the simple class name of the task that produced these results
     */
    @Override
    public String taskName() {
        return taskName;
    }

    /**
     * @return immutable map of extra attributes written by the task
     */
    @Override
    public Map<String, Object> extraAttributes() {
        return extraAttributes;
    }

    /**
     * @return when this task was executed
     */
    @Override
    public Instant processedAt() {
        return processedAt;
    }
}
