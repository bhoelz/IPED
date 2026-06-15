package iped.engine.webapi;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Jersey {@link ContainerResponseFilter} that injects HTTP deprecation headers
 * onto every v1 response so clients have a machine-readable signal to migrate.
 *
 * <p>Header set per
 * <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpapi-deprecation-header">
 * draft-ietf-httpapi-deprecation-header</a>:
 * <ul>
 *   <li>{@code Deprecation: true} — marks the endpoint as deprecated.</li>
 *   <li>{@code Sunset: Sun, 01 Jan 2027 00:00:00 GMT} — target removal date.</li>
 *   <li>{@code Link: </v2/cases>; rel="successor-version"} — canonical replacement.</li>
 * </ul>
 *
 * <p>v1 routes are identified by their path prefix: anything that is NOT under
 * {@code /v2/} and NOT a root/docs route ({@code /}, {@code /swagger}, {@code /openapi}).
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class V1DeprecationFilter implements ContainerResponseFilter {

    private static final String SUNSET_DATE = "Sun, 01 Jan 2027 00:00:00 GMT";

    /** Paths that are infrastructure/docs — never flagged as v1. */
    private static final java.util.Set<String> INFRA_PREFIXES = java.util.Set.of(
            "/v2/", "/swagger", "/openapi", "/webjars"
    );

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext resp) {
        String path = req.getUriInfo().getRequestUri().getPath();

        if (isV1(path)) {
            resp.getHeaders().putSingle("Deprecation",  "true");
            resp.getHeaders().putSingle("Sunset",       SUNSET_DATE);
            resp.getHeaders().putSingle("Link",         "</v2/cases>; rel=\"successor-version\"");
        }
    }

    private static boolean isV1(String path) {
        if (path == null) return false;
        for (String infra : INFRA_PREFIXES) {
            if (path.startsWith(infra) || path.equals("/")) return false;
        }
        return true;
    }
}
