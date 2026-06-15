package iped.tasks.spi;

/**
 * Declares how a task manages mutable state, which determines whether it is
 * safe to run in multi-case or distributed scenarios.
 *
 * <p>Task authors report their state model by implementing
 * {@link TaskProvider#stateModel()} (default: {@link #GLOBAL}). The
 * distributed coordinator uses this to decide how to partition work.
 *
 * <h3>What "state" means here</h3>
 * State is any field (static or instance) that survives an individual
 * {@code process(IItem)} call: counters, caches, open file handles,
 * index writers, result sets accumulated for {@code finish()}.
 */
public enum TaskStateModel {

    /**
     * The task holds no cross-item state. Every {@code process(IItem)} call is
     * fully self-contained. Static fields (if any) are read-only constants.
     *
     * <p>Stateless tasks are safe to run on any worker in a distributed setup
     * without coordination — work units can be freely re-assigned.
     */
    STATELESS,

    /**
     * The task accumulates per-case state (e.g., a report entry list, an index
     * writer) that is initialized in {@code init()}, used across multiple
     * {@code process()} calls, and flushed in {@code finish()}.
     *
     * <p>All instance state is scoped to one case; there are no mutable statics
     * that bleed across case boundaries. Safe for multi-case sequential runs, and
     * for distributed workers as long as each worker owns exactly one case context.
     */
    CASE_SCOPED,

    /**
     * The task uses mutable static fields shared across task instances and/or
     * cases (e.g., a static cache, a global counter, a single shared writer).
     *
     * <p>This is the <em>default</em>. Tasks in this category must not run
     * concurrently across case boundaries until the statics are encapsulated in a
     * {@code CaseContext}-aware holder. See Phase 3 of the iped-tasks roadmap for
     * the migration plan.
     */
    GLOBAL
}
