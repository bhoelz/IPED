package iped.engine.webapi;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Jersey {@link ContainerRequestFilter} that enforces per-source access control.
 *
 * <p>When {@link AllowedSources} is configured (via {@code iped.webapi.allowed-sources}
 * system property or {@code IPED_WEBAPI_ALLOWED_SOURCES} environment variable), this
 * filter rejects requests for source IDs not in the allow-list with {@code 403 Forbidden}.
 *
 * <p>The filter intercepts paths of the form:
 * <ul>
 *   <li>{@code /v2/sources/{sourceId}/…} — v2 item/content/text endpoints</li>
 *   <li>{@code /sources/{sourceID}/…}    — v1 source endpoints</li>
 *   <li>{@code /v2/cases/{id}/…}         — v2 case-scoped endpoints (case ID = source ID)</li>
 *   <li>{@code /v2/cases/{id}}           — single-case GET/DELETE</li>
 * </ul>
 *
 * <p>Runs at {@link Priorities#AUTHORIZATION}{@code + 1} so it executes after the
 * API-key check ({@link ApiKeyAuthFilter}) and after audit logging
 * ({@link AuditLoggingFilter}), ensuring denied source accesses are still audited.
 */
@Provider
@Priority(Priorities.AUTHORIZATION + 1)
public class SourceAccessFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (AllowedSources.get() == null) return; // unrestricted

        String rawPath = ctx.getUriInfo().getRequestUri().getRawPath();
        String sourceId = extractSourceId(rawPath);
        if (sourceId == null) return; // not a source-scoped path

        if (!AllowedSources.isAllowed(sourceId)) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(Map.of("error", "Access to source '" + sourceId + "' is not allowed"))
                    .build());
        }
    }

    /**
     * Extracts the source/case ID from a decoded URL path, or {@code null} when the
     * path is not a source-scoped endpoint.
     *
     * <p>Package-private for unit testing.
     */
    static String extractSourceId(String rawPath) {
        // strip leading slash
        String path = (rawPath != null && rawPath.startsWith("/")) ? rawPath.substring(1) : rawPath;
        if (path == null || path.isEmpty()) return null;

        String[] segs = path.split("/", -1);
        if (segs.length < 2) return null;

        // /v2/sources/{sourceId}/...
        if (segs.length >= 3 && "v2".equals(segs[0]) && "sources".equals(segs[1])) {
            String id = percentDecode(segs[2]);
            return id.isBlank() ? null : id;
        }
        // /sources/{sourceID}/... (v1)
        if ("sources".equals(segs[0])) {
            String id = percentDecode(segs[1]);
            return id.isBlank() ? null : id;
        }
        // /v2/cases/{id} or /v2/cases/{id}/...
        if (segs.length >= 3 && "v2".equals(segs[0]) && "cases".equals(segs[1])) {
            String id = percentDecode(segs[2]);
            return id.isBlank() ? null : id;
        }
        return null;
    }

    private static String percentDecode(String segment) {
        try {
            return java.net.URLDecoder.decode(segment, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return segment;
        }
    }
}
