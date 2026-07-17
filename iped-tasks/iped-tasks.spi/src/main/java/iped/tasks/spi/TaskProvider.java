package iped.tasks.spi;

import java.util.List;

/**
 * Service-provider interface for IPED processing tasks.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} from {@code
 * META-INF/services/iped.tasks.spi.TaskProvider} entries in each task module's JAR.
 *
 * <h3>Lifecycle</h3>
 *
 * The engine creates one task instance per worker thread by calling {@link #createTask()} once per
 * thread. Each instance then goes through three ordered phases:
 *
 * <ol>
 *   <li><strong>{@code init(ConfigurationManager)}</strong> — called once at worker startup. Read
 *       configuration, open files, warm caches. Multiple workers call {@code init} concurrently
 *       (each on its own instance), so any shared/static state must be guarded with
 *       synchronization.
 *   <li><strong>{@code process(IItem)}</strong> — called once per item on a single thread. The
 *       framework guarantees that {@code process} is never called concurrently on the same
 *       instance. Items may be re-enqueued for deferred processing via {@code reEnqueueItem}.
 *   <li><strong>{@code finish()}</strong> — called once at worker shutdown. Flush buffers, close
 *       writers, release external connections opened in {@code init}. Called from the same thread
 *       that owns the instance.
 * </ol>
 *
 * <h3>Instance model</h3>
 *
 * Because each worker thread owns a distinct instance, task implementations should keep all mutable
 * state as instance fields and avoid statics wherever possible. When shared state is unavoidable
 * (e.g., a shared index writer), it must be accessed under explicit synchronization.
 *
 * <h3>Metrics</h3>
 *
 * Tasks that want to expose runtime metrics should additionally implement {@link TaskMetrics}. The
 * engine discovers the interface via {@code instanceof} checks and polls it periodically for the
 * observability pipeline.
 *
 * <h3>Dependencies</h3>
 *
 * Ordering constraints between tasks are declared via {@link TaskDescriptor#dependencies()}. Use
 * {@link TaskDependency#requires(String)} to declare a hard dependency, {@link
 * TaskDependency#before(String)} / {@link TaskDependency#after(String)} for relative ordering
 * without an existence requirement.
 *
 * @param <T> the concrete task type produced by this provider
 */
public interface TaskProvider<T> {

  TaskDescriptor descriptor();

  T createTask();

  /**
   * Declares the task's state model, which determines whether it is safe to run in multi-case or
   * distributed scenarios.
   *
   * <p>Override this method to declare that your task is {@link TaskStateModel#STATELESS} or {@link
   * TaskStateModel#CASE_SCOPED}; the default is {@link TaskStateModel#GLOBAL} which is the most
   * conservative and prevents concurrent multi-case execution.
   *
   * @return this task's state model declaration
   */
  default TaskStateModel stateModel() {
    return TaskStateModel.GLOBAL;
  }

  default List<TaskDependency> dependencies() {
    return descriptor().dependencies();
  }
}
