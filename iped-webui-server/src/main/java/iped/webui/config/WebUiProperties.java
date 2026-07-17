package iped.webui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the web UI server.
 *
 * @param apiBaseUrl base URL of the backing Jersey {@code iped-webapi} that {@code /api/**}
 *     requests are proxied to.
 */
@ConfigurationProperties(prefix = "iped.webui")
public record WebUiProperties(
    String apiBaseUrl,
    Boolean bookmarksEnabled,
    Boolean viewerSearchEnabled,
    Boolean exportEnabled,
    Boolean companionFallbackEnabled) {

  public WebUiProperties {
    if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
      apiBaseUrl = "http://localhost:8080";
    }
    // normalise: no trailing slash, so path joining is predictable
    while (apiBaseUrl.endsWith("/")) {
      apiBaseUrl = apiBaseUrl.substring(0, apiBaseUrl.length() - 1);
    }
  }

  public boolean isBookmarksEnabled() {
    return bookmarksEnabled == null || bookmarksEnabled;
  }

  public boolean isViewerSearchEnabled() {
    return viewerSearchEnabled == null || viewerSearchEnabled;
  }

  public boolean isExportEnabled() {
    return exportEnabled == null || exportEnabled;
  }

  public boolean isCompanionFallbackEnabled() {
    return companionFallbackEnabled == null || companionFallbackEnabled;
  }
}
