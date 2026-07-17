package iped.engine.webapi.spi;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface RenditionService {

  /**
   * Lists rendition kinds available for the given item together with their URL paths so the caller
   * can build absolute URLs.
   *
   * @param sourceId source identifier
   * @param itemId item numeric ID
   * @param mimeType MIME type of the item
   * @param baseUrl base URL prefix for building rendition URLs (e.g. "http://localhost:8080")
   */
  List<RenditionDescriptor> available(String sourceId, int itemId, String mimeType, String baseUrl);

  /**
   * Returns the viewer identifier and capabilities for the given MIME type. Map keys: "viewerId",
   * "search" (bool), "hitsMode" (none|external|internal), "toolbarSupported" (bool),
   * "toolbarVisibleByDefault" (bool).
   */
  Map<String, Object> capabilities(String sourceId, int itemId, String mimeType);

  /**
   * Streams the requested rendition kind for the item into {@code out}.
   *
   * @param kind "text", "html", "image", "pdf", or "bytes"
   * @param page 0-based page index (-1 = whole document)
   * @param width pixel width hint for image renditions (0 = renderer default)
   */
  void writeRendition(
      String sourceId, int itemId, String kind, int page, int width, OutputStream out)
      throws Exception;
}
