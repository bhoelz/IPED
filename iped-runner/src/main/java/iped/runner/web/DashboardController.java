package iped.runner.web;

import iped.runner.execution.ExecutionService;
import iped.runner.execution.JobSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import views.runner.dashboard;

/**
 * Serves the IPED Processing Dashboard — a live view of all running and recently completed
 * processing jobs.
 *
 * <ul>
 *   <li>{@code GET /dashboard} — Rocker SSR shell + React island
 *   <li>{@code GET /dashboard/jobs} — JSON snapshot of all jobs (initial page load)
 *   <li>{@code GET /dashboard/stream} — SSE stream; pushes {@code jobs-update} every ~2 s
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

  @Value("${runner.dev:false}")
  private boolean dev;

  private final ExecutionService executionService;

  @GetMapping(path = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String dashboard() {
    return dashboard.template(dev).render().toString();
  }

  @GetMapping(path = "/dashboard/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<JobSnapshot> jobs() {
    return executionService.snapshots();
  }

  @GetMapping(path = "/dashboard/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream() {
    return executionService.subscribeDashboard();
  }
}
