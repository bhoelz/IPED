package iped.datasource;

import iped.data.AdditionalItemData;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stores and retrieves results of additional processing tasks executed
 * post-indexing on selected items.
 * <p>
 * Each record is keyed by {@code (itemId, taskName)} and holds the extra
 * attributes written by the task.  Calling
 * {@link #storeTaskResult(int, String, Map)} with the same key overwrites any
 * previous result (upsert semantics), making re-running a task idempotent.
 * </p>
 */
public interface IAdditionalDataSource extends Closeable {

    /**
     * Persists (or overwrites) the result of a task run on a specific item.
     *
     * @param itemId     IPED item identifier (stable, not a Lucene doc ID)
     * @param taskName   simple class name of the task
     * @param extraAttrs extra attributes written by the task (may be empty)
     * @throws IOException on I/O failure
     */
    void storeTaskResult(int itemId, String taskName, Map<String, Object> extraAttrs) throws IOException;

    /**
     * Retrieves the result of a single task for an item, if present.
     *
     * @param itemId   IPED item identifier
     * @param taskName simple class name of the task
     * @return the result, or {@link Optional#empty()} if the task was never run
     */
    Optional<AdditionalItemData> getTaskResult(int itemId, String taskName);

    /**
     * Returns whether a task result is stored for the given item.
     *
     * @param itemId   IPED item identifier
     * @param taskName simple class name of the task
     * @return {@code true} if a result exists
     */
    boolean hasTaskResult(int itemId, String taskName);

    /**
     * Returns the names of all tasks that have stored results for the item.
     *
     * @param itemId IPED item identifier
     * @return set of task names (may be empty)
     */
    Set<String> getExecutedTasks(int itemId);

    /**
     * Returns a merged map of all extra attributes contributed by every task
     * that has been run on the given item.  When multiple tasks set the same
     * key the result of the most-recently stored task wins.
     *
     * <p>The default implementation queries each task individually; concrete
     * implementations may override this with a single-query optimisation.</p>
     *
     * @param itemId IPED item identifier
     * @return mutable map (caller may modify freely)
     */
    default Map<String, Object> getMergedExtraAttributes(int itemId) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (String taskName : getExecutedTasks(itemId)) {
            getTaskResult(itemId, taskName)
                    .ifPresent(d -> merged.putAll(d.getExtraAttributes()));
        }
        return merged;
    }

    /**
     * Commits pending writes to persistent storage.  Must be called after one
     * or more {@link #storeTaskResult} calls to guarantee durability.
     *
     * @throws IOException on I/O failure
     */
    void commit() throws IOException;

    /** {@inheritDoc} */
    @Override
    void close() throws IOException;
}
