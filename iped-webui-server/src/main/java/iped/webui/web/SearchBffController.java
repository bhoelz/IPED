package iped.webui.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Backend-for-Frontend: adapts the results-grid island's two-step search
 * contract to the single {@code GET /v2/search} call implemented by
 * {@code iped-webapi}.
 *
 * <p>Island contract (what the Angular component calls):
 * <ol>
 *   <li>{@code POST /api/cases/{caseId}/search} → {@code {"searchId":"…"}}</li>
 *   <li>{@code GET /api/cases/{caseId}/search/{searchId}/results?offset=&limit=}
 *       → {@code {searchId, total, page:{offset,limit}, items[]}}</li>
 * </ol>
 *
 * <p>Implementation:
 * <ul>
 *   <li>Step 1 encodes {@code {caseId, query}} into a Base64 token as the
 *       {@code searchId} — no server state needed.</li>
 *   <li>Step 2 decodes the token and proxies to
 *       {@code GET /v2/search?q=…&sourceId={caseId}&offset=&limit=}.</li>
 *   <li>When webapi returns HTTP 503 (no sources open), step 2 returns 503 so the
 *       island can surface a "no cases open" state rather than an empty grid.</li>
 * </ul>
 *
 * <p>These mappings are more specific than the {@link ApiProxyController} catch-all
 * and take precedence over it. They replace {@code SearchStubController} entirely.
 */
@RestController
@Slf4j
public class SearchBffController {

    private static final String NO_SOURCES_BODY =
            "{\"error\":\"No cases are open in iped-webapi. Open a case first.\",\"total\":0,\"items\":[]}";

    private final RestClient apiClient;

    public SearchBffController(RestClient ipedWebapiClient) {
        this.apiClient = ipedWebapiClient;
    }

    /**
     * Step 1: accept a search query and return a stable {@code searchId} token.
     * The token encodes {@code sourceId:query} in Base64 — no session state required.
     */
    @PostMapping(
        path    = "/api/cases/{caseId}/search",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> createSearch(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, Object> body) {

        String query = body != null && body.get("query") instanceof String q ? q : "*";
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (caseId + "\0" + query).getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok(Map.of("searchId", token, "status", "ready"));
    }

    /**
     * Step 2: decode the {@code searchId} token and proxy to webapi v2 search.
     * Returns the same page shape the island expects:
     * {@code {searchId, total, page:{offset,limit}, items[]}}.
     */
    @GetMapping(
        path    = "/api/cases/{caseId}/search/{searchId}/results",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> results(
            @PathVariable String caseId,
            @PathVariable String searchId,
            @RequestParam(defaultValue = "0")  int offset,
            @RequestParam(defaultValue = "25") int limit) {

        // Decode token → (sourceId, query). Fall back to wildcard if token is invalid.
        String query = decodeQuery(caseId, searchId);

        try {
            @SuppressWarnings("unchecked")
            Map<?, ?> v2 = apiClient.get()
                    .uri(u -> u.path("/v2/search")
                               .queryParam("q", query)
                               .queryParam("sourceId", caseId)
                               .queryParam("offset", offset)
                               .queryParam("limit", limit)
                               .build())
                    .retrieve()
                    .onStatus(
                        s -> s.value() == 503,
                        (req, res) -> { /* handled below */ }
                    )
                    .body(Map.class);

            if (v2 == null) {
                return gatewayError("Empty response from iped-webapi /v2/search");
            }

            // Re-shape webapi v2 response → island contract
            long total   = longOf(v2, "total");
            int  retOff  = intOf(v2, "offset", offset);
            int  retLim  = intOf(v2, "limit", limit);
            List<?> raw  = v2.get("items") instanceof List<?> l ? l : List.of();

            // webapi items use camelCase already; pass through as-is
            List<Object> items = new ArrayList<>(raw);

            var result = new LinkedHashMap<String, Object>();
            result.put("searchId", searchId);
            result.put("total", total);
            result.put("page", Map.of("offset", retOff, "limit", retLim));
            result.put("items", items);
            return ResponseEntity.ok(result);

        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("503")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "No cases are open.", "total", 0, "items", List.of()));
            }
            log.warn("iped-webapi search proxy error: {}", e.getMessage());
            return gatewayError(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    private static String decodeQuery(String caseId, String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int sep = decoded.indexOf('\0');
            if (sep >= 0) return decoded.substring(sep + 1);
        } catch (IllegalArgumentException ignored) {
            // token is not our Base64 encoding (e.g. a legacy stub id) — search wildcard
        }
        return "*";
    }

    private static long longOf(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    private static int intOf(Map<?, ?> m, String key, int fallback) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        return fallback;
    }

    private static ResponseEntity<Map<String, Object>> gatewayError(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "iped-webapi unreachable: " + msg, "total", 0, "items", List.of()));
    }
}
