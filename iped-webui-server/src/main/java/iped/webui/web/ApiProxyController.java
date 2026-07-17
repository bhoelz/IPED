package iped.webui.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Transparent JSON proxy: forwards {@code /api/**} to the backing Jersey {@code iped-webapi} so the
 * browser only ever talks to one origin.
 *
 * <p>This keeps the engine as the single source of truth — the SSR server never imports engine
 * classes, it only relays JSON. More specific mappings (e.g. the temporary {@link
 * SearchStubController}) take precedence over this catch-all.
 */
@RestController
public class ApiProxyController {

  private static final String PREFIX = "/api";

  private final RestClient client;

  public ApiProxyController(RestClient ipedWebapiClient) {
    this.client = ipedWebapiClient;
  }

  @RequestMapping("/api/**")
  public ResponseEntity<byte[]> proxy(
      HttpServletRequest request,
      @RequestHeader HttpHeaders headers,
      @RequestBody(required = false) byte[] body) {
    // Resolve the path independently of framework internals (robust across Spring versions):
    // requestURI is the full path incl. any context path, which we strip before the /api prefix.
    String fullPath = request.getRequestURI();
    String ctx = request.getContextPath();
    if (ctx != null && !ctx.isEmpty() && fullPath.startsWith(ctx)) {
      fullPath = fullPath.substring(ctx.length());
    }
    String downstreamPath = fullPath.substring(PREFIX.length());
    String query = request.getQueryString();

    URI uri = URI.create(downstreamPath + (query != null ? "?" + query : ""));
    HttpMethod method = HttpMethod.valueOf(request.getMethod());

    String traceId = MDC.get(ProxyTracingFilter.MDC_KEY);
    RestClient.RequestBodySpec spec =
        client
            .method(method)
            .uri(uri)
            .headers(
                h -> {
                  copyRequestHeaders(headers, h);
                  if (traceId != null) h.set(ProxyTracingFilter.TRACE_HEADER, traceId);
                });
    if (body != null && body.length > 0) {
      spec.body(body);
    }

    try {
      return spec.retrieve()
          .onStatus(
              status -> true,
              (req, res) -> {
                /* relay backend errors verbatim, don't throw */
              })
          .toEntity(byte[].class);
    } catch (ResourceAccessException e) {
      // backing iped-webapi unreachable: surface a clean gateway error
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(("iped-webapi unreachable: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void copyRequestHeaders(HttpHeaders incoming, HttpHeaders out) {
    // strip hop-by-hop / host headers that must not be forwarded
    incoming.forEach(
        (name, values) -> {
          if (name.equalsIgnoreCase(HttpHeaders.HOST)
              || name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
              || name.equalsIgnoreCase(HttpHeaders.CONNECTION)) {
            return;
          }
          out.addAll(name, values);
        });
  }
}
