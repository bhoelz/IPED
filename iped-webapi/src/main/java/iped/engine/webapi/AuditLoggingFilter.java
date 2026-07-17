package iped.engine.webapi;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Jersey {@link ContainerRequestFilter} that records every mutating HTTP request (POST, PUT, PATCH,
 * DELETE) to the {@link AuditLogger}.
 *
 * <p>Runs after {@link ApiKeyAuthFilter} (lower priority number = runs first; {@link
 * jakarta.ws.rs.Priorities#AUTHENTICATION} = 1000, this filter runs at 2000 so auth is checked
 * first and unauthenticated requests are rejected before they are audited).
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1000)
public class AuditLoggingFilter implements ContainerRequestFilter {

  private static final java.util.Set<String> MUTATING_METHODS =
      java.util.Set.of("POST", "PUT", "PATCH", "DELETE");

  @Override
  public void filter(ContainerRequestContext ctx) {
    String method = ctx.getMethod();
    if (!MUTATING_METHODS.contains(method)) {
      return;
    }

    String path = ctx.getUriInfo().getRequestUri().getPath();
    String principal = ctx.getHeaderString("X-Api-Key");
    if (principal == null) {
      String auth = ctx.getHeaderString("Authorization");
      principal = (auth != null) ? "<bearer>" : "<anonymous>";
    } else {
      // Redact key value — only log that a key was present
      principal = "<apikey>";
    }

    AuditLogger.log(
        "http." + method.toLowerCase(),
        Map.of(
            "path", path,
            "principal", principal));
  }
}
