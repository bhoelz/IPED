package iped.webui.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Injects a per-request trace ID into the MDC and propagates it to the downstream iped-webapi proxy
 * call via {@code X-Trace-Id}.
 *
 * <p>Trace flow:
 *
 * <pre>
 *   Browser → [iped-webui-server]         X-Trace-Id header stamped on response
 *              ProxyTracingFilter (MDC)
 *              ApiProxyController         forwards X-Trace-Id to Jersey
 *                → [iped-webapi / Jersey]  logs with the same trace ID
 * </pre>
 *
 * <p>If the incoming request already carries an {@code X-Trace-Id} header (e.g. from a
 * load-balancer or upstream gateway) it is reused unchanged, so the full call chain is observable
 * as a single trace across services.
 *
 * <p>Log4j2 pattern suggestion — add {@code %X{traceId}} to {@code log4j2.xml}:
 *
 * <pre>{@code
 * <PatternLayout pattern="%d %-5p [%X{traceId}] %c{1} — %m%n"/>
 * }</pre>
 */
@Component
@Order(1)
@Slf4j
public class ProxyTracingFilter extends OncePerRequestFilter {

  static final String TRACE_HEADER = "X-Trace-Id";
  static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = request.getHeader(TRACE_HEADER);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString().replace("-", "");
    }
    MDC.put(MDC_KEY, traceId);
    response.setHeader(TRACE_HEADER, traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
