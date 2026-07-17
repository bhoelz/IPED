package iped.webui.web;

import iped.webui.config.WebMapProperties;
import iped.webui.islands.IslandManifest;
import iped.webui.session.FilterState;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import views.workspace.page;

/**
 * Serves the full, server-rendered workspace page. Spring owns this navigational route; the page
 * embeds HTMX regions and the results-grid island as siblings (see {@code
 * views/workspace/page.rocker.html}).
 *
 * <p>Requires a {@code caseId} query parameter that refers to a case already opened in iped-webapi.
 * Missing or blank → redirects to the case picker. Switching to a different case clears the
 * per-session filter state.
 */
@Controller
public class WorkspaceController {

  private final IslandManifest manifest;
  private final FilterState filterState;
  private final WebMapProperties mapProperties;

  public WorkspaceController(
      IslandManifest manifest, FilterState filterState, WebMapProperties mapProperties) {
    this.manifest = manifest;
    this.filterState = filterState;
    this.mapProperties = mapProperties;
  }

  @GetMapping("/")
  public RedirectView root() {
    return new RedirectView("/cases");
  }

  @GetMapping(path = "/workspace", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> workspace(
      @RequestParam(name = "caseId", required = false) String caseId, Authentication auth) {

    if (caseId == null || caseId.isBlank()) {
      return ResponseEntity.status(HttpStatus.FOUND).header("Location", "/cases").build();
    }

    filterState.switchCase(caseId);

    String username = auth != null ? auth.getName() : "unknown";
    String html =
        page.template(
                manifest.styles(),
                manifest.scripts(),
                caseId,
                username,
                mapProperties.getTileUrl(),
                mapProperties.getTileAttribution(),
                mapProperties.getHeight())
            .render()
            .toString();
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }
}
