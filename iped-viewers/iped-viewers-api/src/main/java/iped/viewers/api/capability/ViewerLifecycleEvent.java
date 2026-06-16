package iped.viewers.api.capability;

/**
 * Lifecycle events emitted by a viewer as it opens, renders, and closes.
 *
 * <p>Web viewers dispatch these as typed CustomEvents (Angular island contract).
 * Native viewers implement {@link IViewerLifecycleListener} if lifecycle callbacks
 * are needed in the Swing or companion-app path.
 *
 * <p>Normal happy-path sequence:
 * <pre>
 *   OPEN → LOADING → READY
 * </pre>
 * Error path:
 * <pre>
 *   OPEN → LOADING → ERROR
 * </pre>
 * Close:
 * <pre>
 *   READY → CLOSED   (or ERROR → CLOSED)
 * </pre>
 */
public enum ViewerLifecycleEvent {

    /** Viewer has been asked to open a new item; data fetch not yet started. */
    OPEN,

    /** Data is being fetched from the server or decoded from the stream. */
    LOADING,

    /** Rendering is complete; the viewer is interactive. */
    READY,

    /** Loading or rendering failed. The error detail is passed with the event. */
    ERROR,

    /** Viewer has released its resources and is no longer rendering. */
    CLOSED
}
