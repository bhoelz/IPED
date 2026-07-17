package iped.webui.web;

import iped.webui.web.WorkspaceFragmentController.EvidenceNode;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub v2 JSON endpoints served locally so Angular islands can make real HTTP calls without a live
 * {@code iped-webapi} instance.
 *
 * <p>Spring MVC route specificity ensures these more-specific paths take precedence over the {@link
 * ApiProxyController} catch-all, exactly as {@link SearchBffController} does for the search
 * contract.
 *
 * <p>Delete or feature-flag these when the real {@code iped-webapi} v2 endpoints ship and the proxy
 * can forward to the engine.
 */
@RestController
@Profile("demo")
public class DemoDataController {

  // ── Categories (C-02) ──────────────────────────────────────────────────

  @GetMapping("/api/cases/{caseId}/categories")
  public Map<String, Object> categories(@PathVariable String caseId) {
    List<Map<String, Object>> list =
        WorkspaceFragmentController.DEMO_CATEGORIES.stream()
            .map(
                c ->
                    Map.<String, Object>of(
                        "name", c.name(),
                        "count", c.count(),
                        "query", "category:\"" + c.name() + "\""))
            .toList();
    return Map.of("categories", list, "total", list.size());
  }

  // ── Bookmarks (C-04) ───────────────────────────────────────────────────

  @GetMapping("/api/cases/{caseId}/bookmarks")
  public Map<String, Object> bookmarks(@PathVariable String caseId) {
    List<Map<String, Object>> list =
        WorkspaceFragmentController.DEMO_BOOKMARKS.stream()
            .map(
                b ->
                    Map.<String, Object>of(
                        "name", b.name(),
                        "count", b.count(),
                        "color", b.color(),
                        "query", "bookmark:\"" + b.name() + "\""))
            .toList();
    return Map.of("bookmarks", list, "total", list.size());
  }

  // ── Evidence tree (C-03) ───────────────────────────────────────────────

  @GetMapping("/api/cases/{caseId}/tree")
  public Map<String, Object> tree(
      @PathVariable String caseId, @RequestParam(defaultValue = "") String nodeId) {
    List<EvidenceNode> nodes =
        nodeId.isBlank()
            ? WorkspaceFragmentController.DEMO_EVIDENCE_ROOTS
            : WorkspaceFragmentController.DEMO_EVIDENCE_CHILDREN.getOrDefault(nodeId, List.of());
    return Map.of(
        "nodes",
        nodes.stream()
            .map(
                n ->
                    Map.<String, Object>of(
                        "id", n.id(),
                        "name", n.name(),
                        "hasChildren", n.hasChildren(),
                        "count", n.count()))
            .toList());
  }

  // ── AI filters (C-06) ─────────────────────────────────────────────────

  @GetMapping("/api/cases/{caseId}/ai-filters")
  public Map<String, Object> aiFilters(@PathVariable String caseId) {
    List<Map<String, Object>> clf =
        WorkspaceFragmentController.DEMO_AI_CLASSIFIERS.stream()
            .map(
                c ->
                    Map.<String, Object>of(
                        "id", c.id(),
                        "name", c.name(),
                        "labels",
                            c.labels().stream()
                                .map(
                                    l ->
                                        Map.<String, Object>of(
                                            "name", l.name(),
                                            "count", l.count(),
                                            "query", c.id() + ":\"" + l.name() + "\""))
                                .toList()))
            .toList();
    return Map.of("classifiers", clf);
  }

  // ── Metadata facets (C-05) ────────────────────────────────────────────

  @GetMapping("/api/cases/{caseId}/facets")
  public Map<String, Object> facets(
      @PathVariable String caseId,
      @RequestParam(defaultValue = "mediaType,year,size") String fields) {
    return Map.of(
        "facets",
        Map.of(
            "mediaType",
                List.of(
                    Map.of("value", "application/pdf", "count", 145),
                    Map.of("value", "image/jpeg", "count", 892),
                    Map.of(
                        "value",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "count",
                        134),
                    Map.of("value", "message/rfc822", "count", 567),
                    Map.of("value", "application/x-sqlite3", "count", 23),
                    Map.of("value", "video/mp4", "count", 67),
                    Map.of("value", "text/plain", "count", 234)),
            "year",
                List.of(
                    Map.of("value", "2021", "count", 156),
                    Map.of("value", "2022", "count", 389),
                    Map.of("value", "2023", "count", 712),
                    Map.of("value", "2024", "count", 234),
                    Map.of("value", "2025", "count", 89)),
            "deleted",
                List.of(
                    Map.of("value", "false", "count", 2198),
                    Map.of("value", "true", "count", 241))));
  }

  // ── Item hits (C-12) ──────────────────────────────────────────────────

  @GetMapping("/api/cases/{caseId}/items/{itemId}/hits")
  public Map<String, Object> hits(
      @PathVariable String caseId,
      @PathVariable String itemId,
      @RequestParam(defaultValue = "") String query) {
    if (query.isBlank()) return Map.of("total", 0, "hits", List.of());
    String term = query;
    List<Map<String, Object>> snippets =
        List.of(
            Map.of(
                "page",
                1,
                "before",
                "The document references a ",
                "match",
                term,
                "after",
                " in the opening paragraph."),
            Map.of(
                "page",
                2,
                "before",
                "Further investigation of the ",
                "match",
                term,
                "after",
                " may reveal more context."),
            Map.of(
                "page",
                5,
                "before",
                "A final mention of ",
                "match",
                term,
                "after",
                " appears near the end."));
    return Map.of("total", snippets.size(), "query", query, "hits", snippets);
  }

  // ── Item metadata (C-14) ──────────────────────────────────────────────

  @GetMapping("/api/cases/{caseId}/items/{itemId}/metadata")
  public Map<String, Object> metadata(@PathVariable String caseId, @PathVariable String itemId) {
    return Map.of(
        "itemId",
        itemId,
        "properties",
        Map.ofEntries(
            Map.entry("name", "demo-document.pdf"),
            Map.entry("path", "/evidence/demo-document.pdf"),
            Map.entry("size", "245678"),
            Map.entry("mediaType", "application/pdf"),
            Map.entry("created", "2023-06-15T10:23:45Z"),
            Map.entry("modified", "2024-01-20T08:55:12Z"),
            Map.entry("md5", "d8e8fca2dc0f896fd7cb4cb0031ba249"),
            Map.entry("sha256", "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"),
            Map.entry("category", "Documents"),
            Map.entry("deleted", "false"),
            Map.entry("hasHits", "true"),
            Map.entry("encoding", "UTF-8")));
  }

  // ── Similar-image search support (C-19) ───────────────────────────────

  @GetMapping("/api/cases/{caseId}/items/{itemId}/similar")
  public Map<String, Object> similar(@PathVariable String caseId, @PathVariable String itemId) {
    return Map.of("query", "hash:\"demo-image-hash\"", "field", "imageHash");
  }
}
