package iped.viewers.api.capability;

/**
 * Readiness classification for serving a viewer in the browser UI.
 *
 * <p>Used in the classification matrix (see {@code specs/95-viewer-classification-matrix.md}) to
 * track which existing Swing viewers can be promoted to web viewers and which require the
 * companion-app bridge path.
 */
public enum ViewerClassification {

  /**
   * The viewer's rendering can be served directly in the browser with no extra work. A web island
   * already exists or the required backend endpoints are in place.
   *
   * <p>Examples: text, image, HTML (sanitized), PDF (via PDF.js or {@code <embed>}), basic hex
   * viewer island.
   */
  WEB_PORTABLE,

  /**
   * The viewer's content type is browser-renderable in principle, but extra work is required before
   * it is ready — e.g. format conversion, sanitisation pipeline, missing island component, or
   * security review.
   *
   * <p>Examples: TIFF (needs server-side conversion), MSG/EML (needs HTML conversion), audio/video
   * (needs codec negotiation or transcoding fallback).
   */
  WEB_PORTABLE_WITH_WORK,

  /**
   * The viewer requires native execution that cannot be reproduced in the browser. These viewers
   * remain in the Swing desktop app or are bridged via the companion app.
   *
   * <p>Examples: LibreOffice embedding, CAD viewer (proprietary SDKs), ReferencedFileViewer (local
   * file system access).
   */
  NATIVE_ONLY
}
