package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.webapi.JobRegistry.JobEntry;
import iped.engine.webapi.JobRegistry.JobEvent;
import java.io.IOException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * SSE endpoint for real-time job progress.
 *
 * <p>Subscribe with:
 *
 * <pre>
 * GET /v2/jobs/{id}/events
 * Accept: text/event-stream
 * </pre>
 *
 * <p>Immediately delivers the current status event so the client never has to poll first.
 * Subsequent events are pushed as the job progresses. The stream closes automatically when the job
 * reaches a terminal state.
 */
@Api(value = "Jobs v2")
@Path("v2/jobs/{id}/events")
public class JobsSseV2 {

  @Context private Sse sse;

  @ApiOperation("SSE stream for real-time job progress")
  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public void events(@PathParam("id") String id, @Context SseEventSink sink) throws IOException {
    var jobOpt = JobRegistry.get(id);
    if (jobOpt.isEmpty()) {
      OutboundSseEvent notFound =
          sse.newEventBuilder()
              .name("error")
              .data("{\"error\":\"Job not found: " + id + "\"}")
              .build();
      sink.send(notFound);
      sink.close();
      return;
    }

    JobEntry job = jobOpt.get();

    // Consumer runs on whatever thread the job uses — sink.send() is thread-safe.
    java.util.function.Consumer<JobEvent> consumer =
        event -> {
          if (sink.isClosed()) return;
          OutboundSseEvent sseEvent =
              sse.newEventBuilder()
                  .name(event.event())
                  .mediaType(MediaType.APPLICATION_JSON_TYPE)
                  .data(
                      "{\"progress\":"
                          + event.progress()
                          + ",\"message\":"
                          + quote(event.message())
                          + "}")
                  .build();
          sink.send(sseEvent);
          if ("completed".equals(event.event())
              || "failed".equals(event.event())
              || "cancelled".equals(event.event())) {
            sink.close();
          }
        };

    job.subscribe(consumer);

    // Clean up subscription if the client disconnects.
    sink.send(sse.newEventBuilder().comment("connected").build())
        .exceptionally(
            ex -> {
              job.unsubscribe(consumer);
              return null;
            });
  }

  private static String quote(String s) {
    return "\"" + (s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
  }
}
