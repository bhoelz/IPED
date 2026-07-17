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
 * Jersey {@link ContainerRequestFilter} that enforces API-key authentication on all routes when a
 * key is configured.
 *
 * <p>Configure via system property or environment variable (checked in order):
 *
 * <ol>
 *   <li>{@code iped.webapi.api-key} system property
 *   <li>{@code IPED_WEBAPI_API_KEY} environment variable
 * </ol>
 *
 * <p>When no key is configured the filter is a no-op, preserving the current open-access behaviour
 * for development.
 *
 * <h3>Accepted header formats</h3>
 *
 * <pre>
 * X-Api-Key: &lt;key&gt;
 * Authorization: Bearer &lt;key&gt;
 * </pre>
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuthFilter implements ContainerRequestFilter {

  private static final String CONFIGURED_KEY = resolveKey();

  private static String resolveKey() {
    String prop = System.getProperty("iped.webapi.api-key");
    if (prop != null && !prop.isBlank()) return prop.trim();
    String env = System.getenv("IPED_WEBAPI_API_KEY");
    if (env != null && !env.isBlank()) return env.trim();
    return null;
  }

  @Override
  public void filter(ContainerRequestContext ctx) {
    if (CONFIGURED_KEY == null) {
      return; // auth disabled — dev / open mode
    }

    String provided = extractKey(ctx);
    if (!CONFIGURED_KEY.equals(provided)) {
      ctx.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .type(MediaType.APPLICATION_JSON_TYPE)
              .entity(Map.of("error", "Invalid or missing API key"))
              .build());
    }
  }

  private static String extractKey(ContainerRequestContext ctx) {
    // X-Api-Key header
    String xApiKey = ctx.getHeaderString("X-Api-Key");
    if (xApiKey != null && !xApiKey.isBlank()) return xApiKey.trim();

    // Authorization: Bearer <key>
    String auth = ctx.getHeaderString("Authorization");
    if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return auth.substring(7).trim();
    }
    return null;
  }
}
