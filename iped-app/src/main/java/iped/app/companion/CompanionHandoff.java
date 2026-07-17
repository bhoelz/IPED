package iped.app.companion;

import java.io.IOException;

/**
 * Protocol contract for delegating evidence item display to the IPED companion desktop app.
 *
 * <p>The companion app runs a loopback HTTP server (default port 18743). Implementations POST a
 * {@link CompanionHandoffRequest} to that server. The companion handles the native-only viewer
 * (LibreOffice, CAD, OS shell).
 *
 * <p>See {@code specs/97-companion-handoff-protocol.md} for the full protocol spec.
 */
public interface CompanionHandoff {

  /**
   * Returns {@code true} if the companion app is reachable on the loopback port. A quick TCP probe
   * is used; results may be cached for up to one second.
   */
  boolean isAvailable();

  /**
   * Asks the companion app to open the given item in the appropriate native viewer.
   *
   * @param request the handoff payload
   * @return {@code true} if the companion acknowledged the request ({@code 200 OK}); {@code false}
   *     if the companion is unavailable or has no viewer for the MIME type
   * @throws IOException if the HTTP call itself fails unexpectedly (not a 503/415)
   */
  boolean openItem(CompanionHandoffRequest request) throws IOException;

  /**
   * Returns the protocol version reported by the running companion app, or {@code -1} if the
   * companion is not available.
   */
  default int companionProtocolVersion() {
    return -1;
  }
}
