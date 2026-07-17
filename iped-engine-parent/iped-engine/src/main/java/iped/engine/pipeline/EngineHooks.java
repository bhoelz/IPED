package iped.engine.pipeline;

import iped.pipeline.IItemProcessingListener;
import iped.pipeline.IJobLifecycleListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of job-lifecycle and item-processing listeners attached to the processing engine.
 *
 * <p>This is the clean seam between the engine and distributed/monitoring infrastructure. Instead
 * of importing Kafka or coordinator types directly, the engine fires events through these hooks and
 * leaves the transport-layer wiring to iped-distributed.
 *
 * <h2>Contract</h2>
 *
 * <ul>
 *   <li>Listeners registered here will be invoked synchronously on the thread that triggers the
 *       event (worker thread for item events, manager thread for job events).
 *   <li>Listener implementations MUST be non-blocking. If a listener needs to do I/O, it should
 *       dispatch to its own executor.
 *   <li>Listener exceptions are caught and logged; they do not abort processing.
 * </ul>
 *
 * <p>One {@code EngineHooks} instance is created per {@code CaseContext} so listeners can be scoped
 * to an individual case.
 */
public final class EngineHooks {

  private final List<IJobLifecycleListener> jobListeners = new CopyOnWriteArrayList<>();
  private final List<IItemProcessingListener> itemListeners = new CopyOnWriteArrayList<>();

  /** Registers a job-lifecycle listener. Idempotent if called twice with the same instance. */
  public void addJobLifecycleListener(IJobLifecycleListener listener) {
    if (!jobListeners.contains(listener)) {
      jobListeners.add(listener);
    }
  }

  /** Removes a previously registered job-lifecycle listener. */
  public void removeJobLifecycleListener(IJobLifecycleListener listener) {
    jobListeners.remove(listener);
  }

  /** Registers an item-processing listener. Idempotent if called twice with the same instance. */
  public void addItemProcessingListener(IItemProcessingListener listener) {
    if (!itemListeners.contains(listener)) {
      itemListeners.add(listener);
    }
  }

  /** Removes a previously registered item-processing listener. */
  public void removeItemProcessingListener(IItemProcessingListener listener) {
    itemListeners.remove(listener);
  }

  /**
   * Fires a job lifecycle event to all registered job listeners. Exceptions thrown by individual
   * listeners are swallowed after logging.
   *
   * @param event the lifecycle event; must not be {@code null}
   */
  public void fireJobEvent(iped.pipeline.JobLifecycleEvent event) {
    for (IJobLifecycleListener l : jobListeners) {
      try {
        l.onJobEvent(event);
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(EngineHooks.class)
            .error("Job lifecycle listener threw an exception", e);
      }
    }
  }

  /**
   * Fires an item processing event to all registered item listeners. Exceptions thrown by
   * individual listeners are swallowed after logging.
   *
   * @param event the item processing event; must not be {@code null}
   */
  public void fireItemEvent(iped.pipeline.ItemProcessingEvent event) {
    for (IItemProcessingListener l : itemListeners) {
      try {
        l.onItemProcessed(event);
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(EngineHooks.class)
            .error("Item processing listener threw an exception", e);
      }
    }
  }

  /**
   * @return {@code true} if at least one job listener is registered
   */
  public boolean hasJobListeners() {
    return !jobListeners.isEmpty();
  }

  /**
   * @return {@code true} if at least one item listener is registered
   */
  public boolean hasItemListeners() {
    return !itemListeners.isEmpty();
  }
}
