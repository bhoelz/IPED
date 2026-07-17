package iped.viewers.api.capability;

/**
 * Optional capability-declaration interface for viewers.
 *
 * <p>Implementing this interface lets the viewer framework inspect a viewer's capabilities —
 * supported MIME types, render target, classification, hit/search support — without loading the
 * viewer's full implementation class.
 *
 * <p>{@link iped.viewers.api.AbstractViewer} implementations may implement this interface alongside
 * the Swing contract. Web island viewers should implement it on the Java bridge/descriptor side
 * rather than in TypeScript.
 *
 * <h3>MIME routing</h3>
 *
 * <p>The framework calls {@link #getCapabilityDescriptor()} to determine whether this viewer
 * handles a given MIME type before calling {@code AbstractViewer.isSupportedType(contentType)}.
 *
 * <h3>Lifecycle</h3>
 *
 * <p>Viewers that want to participate in the web lifecycle model (SSE events, island {@code
 * viewerReadyEvent}) should implement {@link IViewerLifecycleListener} in addition to this
 * interface.
 */
public interface IViewerCapability {

  /**
   * Returns an immutable descriptor of this viewer's static capabilities. The descriptor is
   * computed once and cached by the caller.
   */
  ViewerCapabilityDescriptor getCapabilityDescriptor();
}
