package iped.data;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Holds the result of one additional-processing task run on a specific item.
 * Instances are immutable.
 */
public final class AdditionalItemData {

    private final String taskName;
    private final Map<String, Object> extraAttributes;
    private final Instant processedAt;

    public AdditionalItemData(String taskName, Map<String, Object> extraAttributes, Instant processedAt) {
        this.taskName = taskName;
        this.extraAttributes = Collections.unmodifiableMap(extraAttributes);
        this.processedAt = processedAt;
    }

    /** @return the simple class name of the task that produced these results */
    public String getTaskName() {
        return taskName;
    }

    /** @return immutable map of extra attributes written by the task */
    public Map<String, Object> getExtraAttributes() {
        return extraAttributes;
    }

    /** @return when this task was executed */
    public Instant getProcessedAt() {
        return processedAt;
    }
}
