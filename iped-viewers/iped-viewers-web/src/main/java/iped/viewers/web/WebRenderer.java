package iped.viewers.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public interface WebRenderer {

  /** Stable identifier used in viewer session responses (e.g. "html", "image"). */
  String id();

  /** True if this renderer can handle the given MIME type. */
  boolean canHandle(String mimeType);

  /** The rendition kinds this renderer can produce for the given MIME type. */
  Set<RenditionKind> renditions(String mimeType);

  /** Viewer interaction capabilities for the given MIME type. */
  ViewerCapabilities capabilities(String mimeType);

  /**
   * Render the requested item into {@code out}. The caller is responsible for closing {@code out}.
   *
   * @throws UnsupportedRenditionException if the requested kind is not supported
   */
  void render(RenderRequest request, OutputStream out) throws IOException;
}
