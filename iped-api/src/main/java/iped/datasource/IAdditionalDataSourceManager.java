package iped.datasource;

import java.util.*;

/**
 * Manages one or more {@link IAdditionalDataSource} instances associated with
 * a case and provides aggregated access to their contents.
 */
public interface IAdditionalDataSourceManager {

    /**
     * Registers an additional data source.  Sources are consulted in
     * registration order; later entries win on attribute-key conflicts.
     *
     * @param source the source to register
     */
    void register(IAdditionalDataSource source);

    /**
     * @return an unmodifiable view of all registered sources
     */
    List<IAdditionalDataSource> getSources();

    /**
     * Returns {@code true} when at least one data source is registered.
     */
    default boolean hasAnySources() {
        return !getSources().isEmpty();
    }

    /**
     * Returns a merged map of extra attributes contributed by ALL registered
     * sources for the given item.  Sources registered later win on key conflicts.
     *
     * @param itemId IPED item identifier
     * @return mutable map (caller may modify freely)
     */
    default Map<String, Object> getMergedExtraAttributes(int itemId) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (IAdditionalDataSource source : getSources()) {
            merged.putAll(source.getMergedExtraAttributes(itemId));
        }
        return merged;
    }

    /**
     * Returns the union of all task names that have been executed on the item
     * across all registered sources.
     *
     * @param itemId IPED item identifier
     * @return set of task names (might be empty)
     */
    default Set<String> getAllExecutedTasks(int itemId) {
        Set<String> tasks = new LinkedHashSet<>();
        for (IAdditionalDataSource source : getSources()) {
            tasks.addAll(source.getExecutedTasks(itemId));
        }
        return tasks;
    }
}
