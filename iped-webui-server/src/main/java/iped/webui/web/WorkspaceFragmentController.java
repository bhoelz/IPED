package iped.webui.web;

import iped.webui.session.FilterState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import views.workspace.fragments.*;

import java.util.*;

/**
 * HTMX fragment endpoints: return HTML partials that HTMX swaps into
 * server-owned regions of the workspace page.
 *
 * <p>Boundary rule: everything here renders server-owned markup. Rich,
 * self-contained widget state lives in Angular islands instead and talks to
 * {@code /api/**}; the two never co-own a DOM subtree.
 */
@Controller
@Slf4j
public class WorkspaceFragmentController {


    private static final Set<String> SIDEBAR_TABS =
            Set.of("cat", "evid", "coll", "meta", "ai", "filt");
    private static final Set<String> INFO_TABS =
            Set.of("hits", "sub", "par", "dup", "ref", "refby");
    private static final Set<String> VIEWER_MODES =
            Set.of("hex", "text", "meta", "preview");

    // ── Typed display model records ────────────────────────────────────────

    /** Sidebar category tree entry with hit count. */
    public record CategoryEntry(String name, long count) {}

    /** Sidebar bookmark entry with count and display colour. */
    public record BookmarkEntry(String name, long count, String color) {}

    /** Single label within an AI classifier (e.g. "Safe" under "NSFW"). */
    public record AiLabel(String name, long count) {}

    /** AI classifier with its set of result labels. */
    public record AiClassifier(String id, String name, List<AiLabel> labels) {}

    /** A node in the evidence source tree. */
    public record EvidenceNode(String id, String name, boolean hasChildren, long count) {}

    /** A single text-hit snippet around a query term. */
    public record HitSnippet(String before, String match, String after) {}

    /** Wraps a data fetch result with a flag indicating whether it came from the live backend. */
    record FetchResult<T>(T data, boolean fromBackend) {
        static <T> FetchResult<T> live(T data)  { return new FetchResult<>(data, true); }
        static <T> FetchResult<T> demo(T data)  { return new FetchResult<>(data, false); }
    }

    /** Typed display row passed to {@code itemList.rocker.html}. */
    public record ItemRow(String iid, String title, String name,
                          String ext, String sizeStr, String typeDisp) {}

    // ── Static demo / fallback data ────────────────────────────────────────

    static final List<CategoryEntry> DEMO_CATEGORIES = List.of(
        new CategoryEntry("Documents",   345),
        new CategoryEntry("Images",      892),
        new CategoryEntry("Videos",       67),
        new CategoryEntry("Email",      1204),
        new CategoryEntry("Databases",    23),
        new CategoryEntry("Archives",    156),
        new CategoryEntry("Encrypted",     8),
        new CategoryEntry("Executables", 234),
        new CategoryEntry("Deleted",     421),
        new CategoryEntry("Unknown",      89)
    );

    static final List<BookmarkEntry> DEMO_BOOKMARKS = List.of(
        new BookmarkEntry("Relevant",   23, "#e74c3c"),
        new BookmarkEntry("For Review", 45, "#f39c12"),
        new BookmarkEntry("Excluded",   12, "#7f8c8d"),
        new BookmarkEntry("Important",   7, "#e67e22"),
        new BookmarkEntry("Done",       34, "#27ae60")
    );

    static final List<AiClassifier> DEMO_AI_CLASSIFIERS = List.of(
        new AiClassifier("nsfw", "NSFW", List.of(
            new AiLabel("Safe",       2391),
            new AiLabel("Suggestive",   43),
            new AiLabel("Adult",        12)
        )),
        new AiClassifier("faces", "Face Detection", List.of(
            new AiLabel("No faces", 1850),
            new AiLabel("1 face",    342),
            new AiLabel("2+ faces",   87)
        )),
        new AiClassifier("lang", "Document Language", List.of(
            new AiLabel("Portuguese", 156),
            new AiLabel("English",    234),
            new AiLabel("Spanish",     45),
            new AiLabel("Other",       23)
        ))
    );

    static final List<EvidenceNode> DEMO_EVIDENCE_ROOTS = List.of(
        new EvidenceNode("src0", "evidence.dd [disk image]", true, 2439)
    );

    static final Map<String, List<EvidenceNode>> DEMO_EVIDENCE_CHILDREN = Map.of(
        "src0", List.of(
            new EvidenceNode("src0/p0", "NTFS Partition (C:)",  true,  2100),
            new EvidenceNode("src0/p1", "Unallocated space",    false,  339)
        ),
        "src0/p0", List.of(
            new EvidenceNode("src0/p0/Win",  "C:\\Windows",       true, 456),
            new EvidenceNode("src0/p0/Usr",  "C:\\Users",         true, 1234),
            new EvidenceNode("src0/p0/Prog", "C:\\Program Files", true, 321),
            new EvidenceNode("src0/p0/Tmp",  "C:\\Temp",          false, 89)
        ),
        "src0/p0/Usr", List.of(
            new EvidenceNode("src0/p0/Usr/John",   "John",   true,  789),
            new EvidenceNode("src0/p0/Usr/Public", "Public", false,  45)
        )
    );

    // ── Constructor ────────────────────────────────────────────────────────

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
    public String sidebar(@RequestParam(defaultValue = "cat") String tab,
                          @RequestParam(defaultValue = "demo-case") String caseId) {
        String active = SIDEBAR_TABS.contains(tab) ? tab : "cat";
        var catsResult = active.equals("cat")  ? fetchCategories(caseId) : FetchResult.demo(List.<CategoryEntry>of());
        var bmsResult  = active.equals("coll") ? fetchBookmarks(caseId)  : FetchResult.demo(List.<BookmarkEntry>of());
        List<AiClassifier> clf  = active.equals("ai")   ? DEMO_AI_CLASSIFIERS : List.of();
        List<EvidenceNode> evid = active.equals("evid") ? DEMO_EVIDENCE_ROOTS : List.of();

        String content = sidebar.template(active, caseId,
                catsResult.data(), bmsResult.data(), clf, evid, filterState.list())
                .render().toString();

        // Surface a warning banner when data came from demo fallback (backend unreachable)
        if (!catsResult.fromBackend() && active.equals("cat")
                || !bmsResult.fromBackend() && active.equals("coll")) {
            content = backendUnavailableBanner() + content;
        }
        return content;
    }

    /** HTMX fragment: lazy-loads child nodes of one evidence tree node. */
    @GetMapping(path = "/workspace/sidebar/evid-children", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String evidenceChildrenFragment(@RequestParam String nodeId,
                                           @RequestParam(defaultValue = "demo-case") String caseId) {
        List<EvidenceNode> children = fetchEvidenceChildren(caseId, nodeId);
        return evidenceChildren.template(caseId, children).render().toString();
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
    public String removeFilter(@org.springframework.web.bind.annotation.PathVariable String id) {
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
    public String infoPanel(@RequestParam(defaultValue = "hits") String tab,
                            @RequestParam(required = false) String itemId,
                            @RequestParam(defaultValue = "demo-case") String caseId) {
        String active = INFO_TABS.contains(tab) ? tab : "hits";
        String safeId = itemId == null ? "" : itemId;
        Map<String, String> metadata = fetchMetadata(safeId);
        return infoPanel.template(active, safeId, caseId, metadata).render().toString();
    }

    /** Hits tab fragment: paginated text snippets around query terms. */
    @GetMapping(path = "/workspace/info/hits", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String infoHits(@RequestParam(required = false) String itemId,
                           @RequestParam(defaultValue = "demo-case") String caseId,
                           @RequestParam(defaultValue = "") String query,
                           @RequestParam(defaultValue = "0") int index) {
        String safeId = itemId == null ? "" : itemId;
        List<HitSnippet> snippets = fetchHits(caseId, safeId, query);
        int cur = snippets.isEmpty() ? 0 : Math.max(0, Math.min(index, snippets.size() - 1));
        return hitsPanel.template(safeId, caseId, query, snippets, cur).render().toString();
    }

    // ── Info panel — relational item list ─────────────────────────────────

    private static final Set<String> RELATIONAL_TABS = Set.of("sub", "par", "dup", "ref", "refby");
    private static final int ITEMS_PAGE_LIMIT = 50;

    /**
     * Loads a paginated list of items related to {@code itemId} for one of the
     * relational info-panel tabs. Proxies to the webapi endpoints; gracefully
     * returns an empty list when the endpoint is unavailable (v2 API pending).
     */
    @GetMapping(path = "/workspace/info/items", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String infoItems(@RequestParam String tab,
                            @RequestParam(required = false) String itemId,
                            @RequestParam(defaultValue = "0") int offset) {
        String active = RELATIONAL_TABS.contains(tab) ? tab : "sub";
        String safeId = itemId == null ? "" : itemId;
        DocRef ref = parseItemId(safeId);
        List<ItemRow> items = ref != null
                ? fetchRelatedItems(ref, active, offset, ITEMS_PAGE_LIMIT)
                : List.of();
        int total = items.size() < ITEMS_PAGE_LIMIT ? offset + items.size() : -1;
        boolean hasMore = items.size() == ITEMS_PAGE_LIMIT;
        return itemList.template(active, safeId, items, hasMore, total).render().toString();
    }

    // ── Viewer ─────────────────────────────────────────────────────────────

    /** Viewer-panel content for the selected item. */
    @GetMapping(path = "/workspace/viewer", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String viewer(@RequestParam(defaultValue = "preview") String mode,
                         @RequestParam(required = false) String itemId) {
        String active = VIEWER_MODES.contains(mode) ? mode : "preview";
        String safeId = itemId == null ? "" : itemId;
        boolean needsMeta = active.equals("meta") || active.equals("preview");
        var meta = needsMeta ? fetchMetadataResult(safeId) : FetchResult.live(Map.<String,String>of());
        String mediaType = active.equals("preview") ? extractMediaType(meta.data()) : "";
        String content = viewer.template(active, safeId, meta.data(), mediaType).render().toString();
        if (active.equals("meta") && !safeId.isBlank() && !meta.fromBackend()) {
            content = itemErrorFragment("Could not load metadata from iped-webapi.") + content;
        }
        return content;
    }

    private static String extractMediaType(Map<String, String> metadata) {
        String mt = metadata.get("Media type");
        if (mt == null) mt = metadata.get("mediaType");
        return mt != null ? mt : "";
    }

    // ── Export dialog ──────────────────────────────────────────────────────

    @GetMapping(path = "/workspace/export/dialog", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String exportDialogFragment() {
        return exportDialog.template().render().toString();
    }

    @PostMapping(path = "/workspace/export", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String startExport(@RequestParam(defaultValue = "checked") String scope,
                              @RequestParam(defaultValue = "zip") String format) {
        return """
                <div class="modal-backdrop" onclick="document.getElementById('modal').innerHTML=''"></div>
                <div class="modal-box" role="dialog" aria-modal="true" aria-label="Export started">
                  <div class="modal-header">
                    <span style="font-weight:600">Export queued</span>
                    <button class="iconbtn" style="margin-left:auto"
                            onclick="document.getElementById('modal').innerHTML=''"
                            title="Close">
                      <svg class="icn" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 6 6 18M6 6l12 12"/></svg>
                    </button>
                  </div>
                  <div class="modal-body" style="padding:16px 20px">
                    <div style="display:flex;align-items:center;gap:10px;margin-bottom:12px">
                      <span class="pulse"></span>
                      <span>Preparing <strong>%s</strong> export (%s)…</span>
                    </div>
                    <div style="font-size:11.5px;color:var(--text-dim)">
                      Async export jobs (EPIC-WEB-04) are not yet wired to the engine.
                      This will trigger a real download once the job API lands.
                    </div>
                  </div>
                  <div class="modal-footer">
                    <button class="btn-accent" onclick="document.getElementById('modal').innerHTML=''">Close</button>
                  </div>
                </div>
                """.formatted(format.toUpperCase(), scope);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Parses "{sourceId}:{docId}" item IDs.
     * Returns {@code null} for opaque stub IDs without a colon separator.
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

    @SuppressWarnings("unchecked")
    private FetchResult<List<CategoryEntry>> fetchCategories(String caseId) {
        // v2: GET /v2/sources/{sourceId}/items/categories → String[]
        try {
            List<?> list = apiClient.get()
                    .uri("/v2/sources/{sid}/items/categories", caseId)
                    .retrieve()
                    .body(List.class);
            if (list != null && !list.isEmpty()) {
                return FetchResult.live(list.stream().map(s -> new CategoryEntry(s.toString(), -1L)).toList());
            }
        } catch (RestClientException e) {
            log.debug("Categories unavailable for {}: {}", caseId, e.getMessage());
        }
        return FetchResult.demo(DEMO_CATEGORIES);
    }

    @SuppressWarnings("unchecked")
    private FetchResult<List<BookmarkEntry>> fetchBookmarks(String caseId) {
        try {
            Map<?, ?> body = apiClient.get().uri("/v2/bookmarks").retrieve().body(Map.class);
            if (body != null && body.get("bookmarks") instanceof List<?> list) {
                return FetchResult.live(list.stream()
                        .map(s -> new BookmarkEntry(s.toString(), -1L, "#888")).toList());
            }
        } catch (RestClientException e) {
            log.debug("Bookmarks unavailable: {}", e.getMessage());
        }
        return FetchResult.demo(DEMO_BOOKMARKS);
    }

    private List<AiClassifier> fetchAiFilters(String caseId) {
        return DEMO_AI_CLASSIFIERS;
    }

    private List<EvidenceNode> fetchEvidenceRoots(String caseId) {
        return DEMO_EVIDENCE_ROOTS;
    }

    private List<EvidenceNode> fetchEvidenceChildren(String caseId, String nodeId) {
        return DEMO_EVIDENCE_CHILDREN.getOrDefault(nodeId, List.of());
    }

    private List<HitSnippet> fetchHits(String caseId, String itemId, String query) {
        if (itemId.isBlank()) return List.of();
        // Try v2 webapi hits endpoint when available
        try {
            @SuppressWarnings("unchecked")
            Map<?, ?> body = apiClient.get()
                    .uri("/cases/{c}/items/{i}/hits?query={q}", caseId, itemId, query)
                    .retrieve().body(Map.class);
            if (body != null && body.get("hits") instanceof List<?> rawHits) {
                List<HitSnippet> result = new ArrayList<>();
                for (Object h : rawHits) {
                    if (!(h instanceof Map<?, ?> hm)) continue;
                    result.add(new HitSnippet(
                        str(hm, "before"), str(hm, "match"), str(hm, "after")));
                }
                if (!result.isEmpty()) return result;
            }
        } catch (RestClientException e) {
            log.debug("Hits endpoint unavailable: {}", e.getMessage());
        }
        // Demo: generate fake snippets so the hits tab is visually useful
        String term = query.isBlank() ? itemId.replaceFirst(".*:", "") : query;
        return List.of(
            new HitSnippet("The document references a ", term, " in the opening paragraph."),
            new HitSnippet("Further investigation of the ", term, " may reveal more context."),
            new HitSnippet("A final mention of ", term, " appears on the last page.")
        );
    }

    private static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
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

    private static String tabToApiSegment(String tab) {
        return switch (tab) {
            case "sub"   -> "subitems";
            case "par"   -> "parent";
            case "dup"   -> "duplicates";
            case "ref"   -> "references";
            case "refby" -> "referencedby";
            default      -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private List<ItemRow> fetchRelatedItems(DocRef ref, String tab, int offset, int limit) {
        String segment = tabToApiSegment(tab);
        if (segment == null) return List.of();
        try {
            Map<?, ?> body = apiClient.get()
                    .uri("/sources/{src}/docs/{id}/{seg}?offset={off}&limit={lim}",
                         ref.sourceId(), ref.docId(), segment, offset, limit)
                    .retrieve().body(Map.class);
            if (body == null) return List.of();
            Object raw = body.get("items");
            if (!(raw instanceof List<?> list)) return List.of();
            List<ItemRow> result = new ArrayList<>();
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> m)) continue;
                Map<String, String> flat = new LinkedHashMap<>();
                m.forEach((k, v) -> {
                    if (v == null) return;
                    String val = (v instanceof List<?> l && !l.isEmpty()) ? l.get(0).toString()
                               : v.toString();
                    flat.put(k.toString(), val);
                });
                String iid  = flat.getOrDefault("itemId", "");
                String path = flat.getOrDefault("path", iid);
                String name = flat.getOrDefault("name", path);
                int dot = name.lastIndexOf('.');
                String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
                String mt  = flat.getOrDefault("mediaType", "");
                int sl     = mt.lastIndexOf('/');
                result.add(new ItemRow(iid, path, name, ext,
                                       fmtSize(flat.get("size")),
                                       sl >= 0 ? mt.substring(sl + 1) : mt));
            }
            return result;
        } catch (RestClientException e) {
            log.debug("Relational API unavailable for {}/{}: {}", tab, ref, e.getMessage());
            return List.of();
        }
    }

    private static String fmtSize(String raw) {
        if (raw == null) return "—";
        try {
            long b = Long.parseLong(raw);
            if (b < 1024) return b + " B";
            if (b < 1_048_576) return String.format("%.1f KB", b / 1024.0);
            if (b < 1_073_741_824) return String.format("%.1f MB", b / 1_048_576.0);
            return String.format("%.2f GB", b / 1_073_741_824.0);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private Map<String, String> fetchMetadata(String itemId) {
        return fetchMetadataResult(itemId).data();
    }

    @SuppressWarnings("unchecked")
    private FetchResult<Map<String, String>> fetchMetadataResult(String itemId) {
        DocRef ref = parseItemId(itemId);
        if (ref == null) return FetchResult.live(Map.of());
        try {
            // v2: GET /v2/sources/{sourceId}/items/{id} → ItemMetadataJSON
            Map<?, ?> body = apiClient.get()
                    .uri("/v2/sources/{src}/items/{id}", ref.sourceId(), ref.docId())
                    .retrieve().body(Map.class);
            if (body == null) return FetchResult.demo(Map.of());

            // Collect the structured typed fields into a flat display map
            Map<String, String> result = new LinkedHashMap<>();
            addField(result, "Name",       body, "name");
            addField(result, "Path",       body, "path");
            addField(result, "Media type", body, "mediaType");
            addField(result, "Size",       body, "size");
            addField(result, "Hash",       body, "hash");
            addField(result, "Modified",   body, "modDate");
            addField(result, "Created",    body, "creationDate");
            addField(result, "Accessed",   body, "accessDate");
            addField(result, "Changed",    body, "changeDate");
            addField(result, "Deleted",    body, "deleted");
            addField(result, "Carved",     body, "carved");

            // Merge the raw Tika metadata map for full forensic attribute access
            Object meta = body.get("metadata");
            if (meta instanceof Map<?, ?> metaMap) {
                metaMap.forEach((k, v) -> {
                    if (k == null) return;
                    String val = (v instanceof List<?> l && !l.isEmpty()) ? l.get(0).toString()
                               : (v != null) ? v.toString() : "";
                    result.putIfAbsent(k.toString(), val);
                });
            }
            return FetchResult.live(result);
        } catch (RestClientException e) {
            log.debug("Could not fetch item metadata for {}: {}", itemId, e.getMessage());
            return FetchResult.demo(Map.of());
        }
    }

    private static void addField(Map<String, String> out, String label, Map<?, ?> src, String key) {
        Object v = src.get(key);
        if (v != null) out.put(label, v.toString());
    }

    // ── Error state helpers ────────────────────────────────────────────────

    /** Inline HTML banner shown when HTMX fragment data fell back to demo because the backend is down. */
    private static String backendUnavailableBanner() {
        return """
                <div style="background:var(--warn-bg,#fffbe6);border:1px solid var(--warn-border,#ffe58f);\
                border-radius:4px;padding:6px 10px;margin:6px 4px;font-size:11.5px;\
                display:flex;align-items:center;gap:6px">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" \
                stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 \
                1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/>\
                <line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                  <span>iped-webapi unreachable — showing demo data</span>
                </div>
                """;
    }

    /** Inline HTML error state for a fragment when the backend returned an error for a specific item. */
    static String itemErrorFragment(String message) {
        return """
                <div style="padding:12px 16px;color:var(--text-dim,#888);font-size:12px;\
                display:flex;align-items:center;gap:8px">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" \
                stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>\
                <line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                  <span>%s</span>
                </div>
                """.formatted(message);
    }
}
