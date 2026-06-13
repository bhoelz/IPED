package iped.pipeline;

/**
 * Callback interface for case/job lifecycle transitions.
 *
 * <p>Implementations receive a {@link JobLifecycleEvent} whenever a processing
 * job transitions between states (queued → started → completed/failed, or
 * paused/resumed). This is the API-level hook point for:
 * <ul>
 *   <li>The distributed coordinator — to update case state without importing
 *       engine or Kafka types.</li>
 *   <li>The runner dashboard — to refresh progress views.</li>
 *   <li>Monitoring/telemetry integrations.</li>
 * </ul>
 *
 * <p>Implementations must be thread-safe; the engine may fire events from any
 * thread.
 */
@FunctionalInterface
public interface IJobLifecycleListener {

    /**
     * Called when a processing job transitions to a new lifecycle phase.
     *
     * @param event the lifecycle event; never {@code null}
     */
    void onJobEvent(JobLifecycleEvent event);
}
