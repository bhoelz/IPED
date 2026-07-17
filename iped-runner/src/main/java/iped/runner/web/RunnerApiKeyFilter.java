package iped.runner.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring {@link OncePerRequestFilter} that enforces API-key authentication for the runner REST API
 * when a key is configured.
 *
 * <p>Mirrors the scheme used in {@code iped-webapi}'s {@code ApiKeyAuthFilter}:
 *
 * <ol>
 *   <li>{@code runner.api-key} Spring property (or {@code RUNNER_API_KEY} env)
 *   <li>Accepted as {@code X-Api-Key: <key>} or {@code Authorization: Bearer <key>}
 *   <li>No-op when no key is configured — preserves open-access dev behaviour.
 * </ol>
 *
 * <p>The dashboard SSE endpoint ({@code /dashboard}) is always accessible so browser-rendered
 * monitoring pages work without key management.
 */
@Component
@Order(1)
public class RunnerApiKeyFilter extends OncePerRequestFilter {

  @Value("${runner.api-key:}")
  private String configuredKey;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    if (configuredKey == null || configuredKey.isBlank()) {
      chain.doFilter(request, response); // open mode
      return;
    }

    // Always allow the SSE dashboard and static assets through.
    String path = request.getRequestURI();
    if (path.startsWith("/dashboard")
        || path.startsWith("/actuator")
        || path.startsWith("/browse")
        || path.equals("/")) {
      chain.doFilter(request, response);
      return;
    }

    String provided = extractKey(request);
    if (configuredKey.equals(provided)) {
      chain.doFilter(request, response);
    } else {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
    }
  }

  private static String extractKey(HttpServletRequest req) {
    String xApiKey = req.getHeader("X-Api-Key");
    if (xApiKey != null && !xApiKey.isBlank()) return xApiKey.trim();

    String auth = req.getHeader("Authorization");
    if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return auth.substring(7).trim();
    }
    return null;
  }
}
