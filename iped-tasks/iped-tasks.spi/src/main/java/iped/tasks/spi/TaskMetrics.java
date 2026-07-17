package iped.tasks.spi;

/**
 * Optional interface that task implementations may also implement to surface per-instance runtime
 * metrics to the observability stack.
 *
 * <p>The engine discovers this interface via {@code instanceof} checks on each object returned by
 * {@link TaskProvider#createTask()} and collects snapshots at a configurable interval. All values
 * are cumulative since {@code init()}.
 *
 * <p>Implementations must be thread-safe: the engine may read these counters from a monitoring
 * thread while {@code process()} runs on the worker thread. Use {@code AtomicLong} or equivalent
 * for the backing fields.
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public class MyTask extends AbstractTask implements TaskMetrics {
 *     private final AtomicLong processed = new AtomicLong();
 *     private final AtomicLong errors    = new AtomicLong();
 *     private final AtomicLong millis    = new AtomicLong();
 *
 *     @Override protected void process(IItem item) throws Exception {
 *         long t = System.currentTimeMillis();
 *         try { doWork(item); processed.incrementAndGet(); }
 *         catch (Exception e) { errors.incrementAndGet(); throw e; }
 *         finally { millis.addAndGet(System.currentTimeMillis() - t); }
 *     }
 *
 *     @Override public long getProcessedCount()  { return processed.get(); }
 *     @Override public long getErrorCount()       { return errors.get(); }
 *     @Override public long getProcessingMillis() { return millis.get(); }
 *     @Override public String getTaskName()       { return "MyTask"; }
 * }
 * }</pre>
 */
public interface TaskMetrics {

  /** Items successfully processed by this task instance since {@code init()}. */
  long getProcessedCount();

  /** Items that caused a non-fatal error in this task instance since {@code init()}. */
  long getErrorCount();

  /**
   * Cumulative wall-clock milliseconds spent inside {@code process()} for this task instance since
   * {@code init()}, including items that later failed.
   */
  long getProcessingMillis();

  /** Human-readable task name, typically matching {@link TaskDescriptor#displayName()}. */
  String getTaskName();

  /** Average throughput in items per second, or 0 if no items processed yet. */
  default double getItemsPerSecond() {
    long ms = getProcessingMillis();
    return ms == 0 ? 0.0 : getProcessedCount() * 1000.0 / ms;
  }
}
