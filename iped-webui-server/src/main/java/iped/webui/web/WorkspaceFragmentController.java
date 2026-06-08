package iped.webui.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import iped.webui.session.FilterState;
import iped.webui.session.FilterState.ActiveFilter;
import views.workspace.fragments.exportDialog;
import views.workspace.fragments.filterChips;
import views.workspace.fragments.infoPanel;
import views.workspace.fragments.sidebar;
import views.workspace.fragments.viewer;

/**
 * HTMX fragment endpoints: return HTML partials that HTMX swaps into
 * server-owned regions of the workspace page.
 *
 * <p>Boundary rule: everything here renders server-owned markup. Rich,
 * self-contained widget state lives in Angular islands instead and talks to
 * {@code /api/**}; the two never co-own a DOM subtree.
 */
@Controller
public class WorkspaceFragmentController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceFragmentController.class);

    private static final Set<String> SIDEBAR_TABS =
            Set.of("cat", "evid", "coll", "meta", "ai", "filt");
    private static final Set<String> INFO_TABS =
            Set.of("hits", "sub", "par", "dup", "ref", "refby");
    private static final Set<String> VIEWER_MODES =
            Set.of("hex", "text", "meta", "preview");

    private final RestClient apiClient;
    private final FilterState filterState;

    public WorkspaceFragmentController(RestClient ipedWebapiClient, FilterState filterState) {
        this.apiClient = ipedWebapiClient;
        this.filterState = filterState;
    }

    // ── Sidebar ────────────────────────────────────────────────────────────

    /** Full sidebar fragment — switches active tab content. */
    @GetMapping(path = "/workspace/sidebar", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String sidebar(@RequestParam(name = "tab", defaultValue = "cat") String tab) {
        String active = SIDEBAR_TABS.contains(tab) ? tab : "cat";
        List<String> categories = active.equals("cat") ? fetchList("/categories") : List.of();
        List<String> bookmarks  = active.equals("coll") ? fetchList("/bookmarks")  : List.of();
        return sidebar.template(active, categories, bookmarks, filterState.list())
                .render().toString();
    }

    // ── Filter CRUD ────────────────────────────────────────────────────────

    /**
     * Add a filter chip. Returns the updated chips fragment so HTMX can swap
     * {@code #filter-chips} in the top bar.
     */
    @PostMapping(path = "/workspace/filter", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String addFilter(@RequestParam String type,
                            @RequestParam String label) {
        String id = (type + "_" + label).replaceAll("[^A-Za-z0-9_]", "_").toLowerCase();
        filterState.put(id, type, label);
        return filterChips.template(filterState.list()).render().toString();
    }

    /** Remove one filter chip. Returns the updated chips fragment. */
    @DeleteMapping(path = "/workspace/filter/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String removeFilter(@PathVariable String id) {
        filterState.remove(id);
        return filterChips.template(filterState.list()).render().toString();
    }

    /** Clear all filter chips. Returns the (now-empty) chips fragment. */
    @DeleteMapping(path = "/workspace/filters", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String clearFilters() {
        filterState.clear();
        return filterChips.template(filterState.list()).render().toString();
    }

    /** Load the current filter chips on page init ({@code hx-trigger="load"}). */
    @GetMapping(path = "/workspace/filter-chips", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String filterChipsFragment() {
        return filterChips.template(filterState.list()).render().toString();
    }

    // ── Info panel ─────────────────────────────────────────────────────────

    /** Info-panel tab content for the currently selected item. */
    @GetMapping(path = "/workspace/info", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String infoPanel(@RequestParam(name = "tab", defaultValue = "hits") String tab,
                            @RequestParam(name = "itemId", required = false) String itemId) {
        String active = INFO_TABS.contains(tab) ? tab : "hits";
        String safeId = itemId == null ? "" : itemId;
        Map<String, String> metadata = fetchMetadata(safeId);
        return infoPanel.template(active, safeId, metadata).render().toString();
    }

    // ── Viewer ─────────────────────────────────────────────────────────────

    /** Viewer-panel content for the selected item. */
    @GetMapping(path = "/workspace/viewer", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String viewer(@RequestParam(name = "mode", defaultValue = "preview") String mode,
                         @RequestParam(name = "itemId", required = false) String itemId) {
        String active = VIEWER_MODES.contains(mode) ? mode : "preview";
        String safeId = itemId == null ? "" : itemId;
        // Only fetch metadata when the metadata tab is actually active (avoid wasted calls).
        Map<String, String> metadata = active.equals("meta") ? fetchMetadata(safeId) : Map.of();
        return viewer.template(active, safeId, metadata).render().toString();
    }

    // ── Export dialog ──────────────────────────────────────────────────────

    @GetMapping(path = "/workspace/export/dialog", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String exportDialogFragment() {
        return exportDialog.template().render().toString();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Parses "{sourceId}:{docId}" item IDs produced by the v2 search contract.
     * Returns {@code null} for opaque stub IDs like {@code "item-5"} that lack
     * the colon separator — callers must treat null as "no real item resolved".
     */
    private record DocRef(String sourceId, int docId) {}

    private static DocRef parseItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        int colon = itemId.lastIndexOf(':');
        if (colon <= 0) return null;
        try {
            return new DocRef(itemId.substring(0, colon),
                              Integer.parseInt(itemId.substring(colon + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Fetch a flat string list from a Jersey endpoint returning {@code {"list":[…]}}. */
    @SuppressWarnings("unchecked")
    private List<String> fetchList(String path) {
        try {
            Map<?, ?> body = apiClient.get().uri(path).retrieve().body(Map.class);
            if (body != null && body.get("list") instanceof List<?> l) {
                return l.stream().map(Object::toString).toList();
            }
        } catch (RestClientException e) {
            log.debug("API unavailable for {}: {}", path, e.getMessage());
        }
        return List.of();
    }

    /**
     * Fetch document properties for {@code itemId} (format "{sourceId}:{docId}")
     * and flatten the Lucene fields into a {@code Map<field, firstValue>}.
     * Returns an empty map for stub IDs or if the backend is unavailable.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> fetchMetadata(String itemId) {
        DocRef ref = parseItemId(itemId);
        if (ref == null) return Map.of();
        try {
            Map<?, ?> body = apiClient.get()
                    .uri("/sources/{src}/docs/{id}", ref.sourceId(), ref.docId())
                    .retrieve().body(Map.class);
            if (body == null) return Map.of();
            Object props = body.get("properties");
            if (!(props instanceof Map<?, ?> propsMap)) return Map.of();
            Map<String, String> result = new LinkedHashMap<>();
            propsMap.forEach((k, v) -> {
                String val = (v instanceof List<?> l && !l.isEmpty()) ? l.get(0).toString()
                           : (v != null) ? v.toString() : "";
                result.put(k.toString(), val);
            });
            return result;
        } catch (RestClientException e) {
            log.debug("Could not fetch metadata for {}: {}", itemId, e.getMessage());
            return Map.of();
        }
    }
}
