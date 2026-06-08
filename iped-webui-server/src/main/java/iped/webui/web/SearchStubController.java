package iped.webui.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY pilot stub for the v2 search contract.
 *
 * <p>The generated client targets {@code POST /cases/{caseId}/search} →
 * {@code SearchResultsPage}, but the live Jersey {@code iped-webapi} still only
 * implements the legacy {@code GET /search?q=} endpoint. Until the v2 contract
 * is implemented against the engine (EPIC-WEB-03 in
 * {@code specs/87-web-ui-delivery-backlog.md}), this stub returns
 * contract-shaped demo data so the results-grid island can be exercised
 * end-to-end.
 *
 * <p>These explicit mappings take precedence over the {@code /api/**} catch-all
 * proxy in {@link ApiProxyController}. Replace with a real backend path and
 * delete this class.
 */
@RestController
public class SearchStubController {

    private static final int DEMO_TOTAL = 137;

    /** Accept a search and hand back a stable searchId (contract: SearchCreateResponse). */
    @PostMapping("/api/cases/{caseId}/search")
    public Map<String, Object> createSearch(@PathVariable String caseId,
                                            @RequestBody(required = false) Map<String, Object> body) {
        String searchId = "stub-" + UUID.nameUUIDFromBytes((caseId + body).getBytes()).toString().substring(0, 8);
        return Map.of("searchId", searchId, "status", "ready");
    }

    /** Page of results (contract: SearchResultsPage). */
    @GetMapping("/api/cases/{caseId}/search/{searchId}/results")
    public Map<String, Object> results(@PathVariable String caseId,
                                       @PathVariable String searchId,
                                       @RequestParam(defaultValue = "0") int offset,
                                       @RequestParam(defaultValue = "25") int limit) {
        int from = Math.max(0, offset);
        int to = Math.min(DEMO_TOTAL, from + Math.max(1, limit));

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = from; i < to; i++) {
            items.add(demoItem(i));
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("searchId", searchId);
        result.put("total", DEMO_TOTAL);
        result.put("page", Map.of("offset", from, "limit", limit));
        result.put("items", items);
        return result;
    }

    private static Map<String, Object> demoItem(int i) {
        String[] exts = {"pdf", "docx", "xlsx", "eml", "jpg", "png", "txt", "db"};
        String[] types = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "message/rfc822",
                "image/jpeg", "image/png", "text/plain", "application/x-sqlite3"};
        int k = i % exts.length;
        var item = new LinkedHashMap<String, Object>();
        item.put("itemId", "item-" + i);
        item.put("sourceId", "src-0");
        item.put("score", Math.round((1.0 - (i % 20) / 25.0) * 100.0) / 100.0);
        item.put("name", String.format("evidence-%04d.%s", i, exts[k]));
        item.put("path", String.format("/Volumes/case/%s/evidence-%04d.%s",
                exts[k].toUpperCase(), i, exts[k]));
        item.put("mediaType", types[k]);
        item.put("size", (long) (1024 + (i * 9973L) % 5_000_000L));
        return item;
    }
}
