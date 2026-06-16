package iped.webui.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.view.RedirectView;
import views.cases.page;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * BFF controller for the case-picker page.
 *
 * <p>Routes:
 * <pre>
 * GET  /cases            — render the case picker (list open cases + open-case form)
 * POST /cases            — open a case by path; on success redirects to /workspace?caseId=…
 * POST /cases/{id}/close — close a case; redirects back to /cases
 * </pre>
 *
 * <p>All backend calls are proxied to {@code iped-webapi /v2/cases}.
 * If the webapi is unreachable the picker still renders with an appropriate banner.
 */
@Controller
@Slf4j
public class CaseBffController {

    private final RestClient apiClient;

    public CaseBffController(RestClient ipedWebapiClient) {
        this.apiClient = ipedWebapiClient;
    }

    /** Display model for one open case in the picker list. */
    public record CaseSummary(String id, String path, String openedAt, long itemCount) {}

    // ── Case picker page ───────────────────────────────────────────────────────

    @GetMapping(path = "/cases", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String casePicker(@RequestParam(required = false) String error) {
        FetchResult result = fetchOpenCases();
        return page.template(result.cases(), result.webapiDown(), error).render().toString();
    }

    // ── Open a case ────────────────────────────────────────────────────────────

    @PostMapping(path = "/cases")
    public RedirectView openCase(
            @RequestParam String path,
            @RequestParam(required = false) String id) {

        Map<String, String> body = new LinkedHashMap<>();
        body.put("path", path.trim());
        if (id != null && !id.isBlank()) {
            body.put("id", id.trim());
        }

        try {
            @SuppressWarnings("unchecked")
            Map<?, ?> result = apiClient.post()
                    .uri("/v2/cases")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String caseId = result != null && result.get("id") instanceof String s ? s : "";
            if (caseId.isBlank()) {
                return errorRedirect("Unexpected response from iped-webapi");
            }
            return new RedirectView("/workspace?caseId=" +
                    URLEncoder.encode(caseId, StandardCharsets.UTF_8));

        } catch (RestClientResponseException e) {
            String msg = extractError(e);
            log.warn("openCase failed ({}): {}", e.getStatusCode(), msg);
            return errorRedirect(msg);
        } catch (RestClientException e) {
            log.warn("openCase failed (network): {}", e.getMessage());
            return errorRedirect("iped-webapi is not reachable");
        }
    }

    // ── Close a case ───────────────────────────────────────────────────────────

    // HTML forms do not support DELETE, so this is a POST action.
    @PostMapping(path = "/cases/{id}/close")
    public RedirectView closeCase(@PathVariable String id) {
        try {
            apiClient.delete()
                    .uri("/v2/cases/{id}", id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("closeCase {} failed: {}", id, e.getMessage());
        }
        return new RedirectView("/cases");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private record FetchResult(List<CaseSummary> cases, boolean webapiDown) {}

    @SuppressWarnings("unchecked")
    private FetchResult fetchOpenCases() {
        try {
            List<?> raw = apiClient.get()
                    .uri("/v2/cases")
                    .retrieve()
                    .body(List.class);
            if (raw == null) return new FetchResult(List.of(), false);

            List<CaseSummary> cases = raw.stream()
                    .filter(e -> e instanceof Map)
                    .map(e -> {
                        Map<?, ?> m = (Map<?, ?>) e;
                        long count = m.get("itemCount") instanceof Number n ? n.longValue() : -1L;
                        return new CaseSummary(
                                str(m, "id"), str(m, "path"), str(m, "openedAt"), count);
                    })
                    .toList();
            return new FetchResult(cases, false);

        } catch (RestClientException e) {
            log.debug("Could not fetch cases from webapi: {}", e.getMessage());
            return new FetchResult(List.of(), true);
        }
    }

    private static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private static RedirectView errorRedirect(String msg) {
        return new RedirectView("/cases?error=" +
                URLEncoder.encode(msg, StandardCharsets.UTF_8));
    }

    private static String extractError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        // Extract "error" value from JSON without a full parse
        int idx = body.indexOf("\"error\"");
        if (idx >= 0) {
            int colon = body.indexOf(':', idx);
            if (colon >= 0) {
                int q1 = body.indexOf('"', colon + 1);
                if (q1 >= 0) {
                    int q2 = body.indexOf('"', q1 + 1);
                    if (q2 > q1) return body.substring(q1 + 1, q2);
                }
            }
        }
        return body.isBlank() ? String.valueOf(e.getMessage()) : body;
    }
}
