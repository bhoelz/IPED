package iped.webui.web;

import iped.webui.islands.IslandManifest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;
import views.workspace.page;

/**
 * Serves the full, server-rendered workspace page. Spring owns this
 * navigational route; the page embeds HTMX regions and the results-grid island
 * as siblings (see {@code views/workspace/page.rocker.html}).
 */
@Controller
public class WorkspaceController {

    private static final String DEMO_CASE = "demo-case";

    private final IslandManifest manifest;

    public WorkspaceController(IslandManifest manifest) {
        this.manifest = manifest;
    }

    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/workspace");
    }

    @GetMapping(path = "/workspace", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String workspace(@RequestParam(name = "caseId", defaultValue = DEMO_CASE) String caseId) {
        return page.template(manifest.styles(), manifest.scripts(), caseId)
                .render()
                .toString();
    }
}
