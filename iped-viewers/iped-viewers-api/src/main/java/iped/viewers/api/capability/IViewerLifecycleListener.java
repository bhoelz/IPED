package iped.viewers.api.capability;

/**
 * Callback interface for receiving viewer lifecycle events.
 *
 * <p>Viewers that participate in the structured lifecycle model (for analytics,
 * tracing, or bridging to the web SSE event stream) should implement this.
 * The framework calls {@link #onViewerLifecycle(ViewerLifecycleEvent, String)}
 * at each transition.
 *
 * <p>Web equivalent: the Angular island dispatches typed {@code CustomEvent}s
 * ({@code viewerReadyEvent}, {@code islandErrorEvent}) that map to this contract.
 */
@FunctionalInterface
public interface IViewerLifecycleListener {

    /**
     * Invoked when the viewer transitions to a new lifecycle state.
     *
     * @param event   the new lifecycle state
     * @param detail  optional human-readable detail (error message on ERROR,
     *                viewer type on READY, {@code null} for other events)
     */
    void onViewerLifecycle(ViewerLifecycleEvent event, String detail);
}
