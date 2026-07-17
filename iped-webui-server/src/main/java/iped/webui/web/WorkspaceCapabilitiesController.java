package iped.webui.web;

import iped.webui.config.WebUiProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes deployment capability flags to the SSR shell and islands. */
@RestController
public class WorkspaceCapabilitiesController {
  private final WebUiProperties properties;

  public WorkspaceCapabilitiesController(WebUiProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/workspace/capabilities")
  public Map<String, Boolean> capabilities() {
    return Map.of(
        "bookmarks",
        properties.isBookmarksEnabled(),
        "viewerSearch",
        properties.isViewerSearchEnabled(),
        "export",
        properties.isExportEnabled(),
        "companionFallback",
        properties.isCompanionFallbackEnabled());
  }
}
