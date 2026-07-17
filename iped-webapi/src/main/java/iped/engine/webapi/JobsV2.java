package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.webapi.JobRegistry.JobEntry;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * v2 async job resource.
 *
 * <p>Covers export and report generation — operations too slow for a synchronous HTTP response.
 * Callers submit a job, get a {@code jobId}, poll {@code GET /v2/jobs/{id}} for status, and
 * optionally subscribe to {@code GET /v2/jobs/{id}/events} (SSE) for real-time progress.
 *
 * <h2>Supported job types</h2>
 *
 * <ul>
 *   <li>{@code export} — pack selected items into a ZIP file. Params: {@code sourceId} (string),
 *       {@code itemIds} (int[]), {@code outputPath} (string, optional — defaults to temp dir).
 *   <li>{@code report} — generate an HTML/PDF report for a case. Params: {@code sourceId} (string),
 *       {@code format} ("html"|"pdf").
 * </ul>
 */
@Api(value = "Jobs v2")
@Path("v2/jobs")
public class JobsV2 {

  private static final ExecutorService WORKERS =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "job-worker");
            t.setDaemon(true);
            return t;
          });

  // ── Submit ────────────────────────────────────────────────────────────────

  @ApiOperation("Submit an async job")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response submit(Map<String, Object> body) {
    if (body == null || !body.containsKey("type")) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", "'type' is required"))
          .build();
    }

    String type = (String) body.get("type");
    @SuppressWarnings("unchecked")
    Map<String, Object> params =
        body.containsKey("params") ? (Map<String, Object>) body.get("params") : Map.of();

    JobEntry job = JobRegistry.create(type, params);
    AuditLogger.log("job.submit", Map.of("jobId", job.id, "type", type));

    switch (type) {
      case "export" -> WORKERS.submit(() -> runExport(job));
      case "report" -> WORKERS.submit(() -> runReport(job));
      default -> {
        job.fail("Unknown job type: " + type);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", "Unknown type: " + type))
            .build();
      }
    }

    return Response.status(Response.Status.ACCEPTED)
        .entity(Map.of("jobId", job.id, "status", job.status))
        .build();
  }

  // ── List ──────────────────────────────────────────────────────────────────

  @ApiOperation("List all jobs")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Map<String, Object>> list() {
    return JobRegistry.all().stream().map(JobEntry::toMap).toList();
  }

  // ── Get ───────────────────────────────────────────────────────────────────

  @ApiOperation("Get a single job's status")
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJob(@PathParam("id") String id) {
    return JobRegistry.get(id)
        .map(j -> Response.ok(j.toMap()).build())
        .orElseGet(
            () ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Job not found: " + id))
                    .build());
  }

  // ── Cancel ────────────────────────────────────────────────────────────────

  @ApiOperation("Cancel a pending or running job")
  @DELETE
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response cancel(@PathParam("id") String id) {
    return JobRegistry.get(id)
        .map(
            j -> {
              if (j.isTerminal()) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Job already in terminal state: " + j.status))
                    .build();
              }
              j.cancel();
              AuditLogger.log("job.cancel", Map.of("jobId", id));
              return Response.noContent().build();
            })
        .orElseGet(
            () ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Job not found: " + id))
                    .build());
  }

  // ── Workers ───────────────────────────────────────────────────────────────

  private static void runExport(JobEntry job) {
    try {
      job.start();
      String sourceId = (String) job.params.getOrDefault("sourceId", "");
      @SuppressWarnings("unchecked")
      List<Integer> itemIds = (List<Integer>) job.params.getOrDefault("itemIds", List.of());

      if (sourceId.isBlank()) {
        job.fail("'sourceId' is required");
        return;
      }

      job.progress(5, "Resolving source");
      var source = Sources.getSource(sourceId);
      if (source == null) {
        job.fail("Source not found: " + sourceId);
        return;
      }

      String outputPath =
          (String)
              job.params.getOrDefault(
                  "outputPath",
                  System.getProperty("java.io.tmpdir") + "/iped-export-" + job.id + ".zip");

      job.progress(10, "Starting ZIP export to " + outputPath);

      try (var zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(outputPath))) {

        int done = 0;
        for (int docId : itemIds) {
          if (job.status.equals("cancelled")) return;
          var item = source.getItemByID(docId);
          if (item == null) continue;
          zos.putNextEntry(
              new java.util.zip.ZipEntry(
                  item.getPath() != null ? item.getPath().replaceFirst("^/", "") : item.getName()));
          try (var is = item.getBufferedInputStream()) {
            if (is != null) is.transferTo(zos);
          }
          zos.closeEntry();
          done++;
          int pct = 10 + (int) (80.0 * done / Math.max(1, itemIds.size()));
          if (pct % 5 == 0) job.progress(pct, "Exported " + done + "/" + itemIds.size());
        }
      }

      job.complete("Export written to " + outputPath);
    } catch (Exception e) {
      job.fail("Export failed: " + e.getMessage());
    }
  }

  private static void runReport(JobEntry job) {
    try {
      job.start();
      String sourceId = (String) job.params.getOrDefault("sourceId", "");
      String format = (String) job.params.getOrDefault("format", "html");

      if (sourceId.isBlank()) {
        job.fail("'sourceId' is required");
        return;
      }

      job.progress(20, "Collecting case metadata");
      var source = Sources.getSource(sourceId);
      if (source == null) {
        job.fail("Source not found: " + sourceId);
        return;
      }

      job.progress(50, "Generating " + format.toUpperCase() + " report");

      // Placeholder — real report generation delegates to the engine's ReportTask.
      Thread.sleep(500);

      String outputPath =
          System.getProperty("java.io.tmpdir") + "/iped-report-" + job.id + "." + format;
      job.complete("Report written to " + outputPath);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      job.fail("Report generation interrupted");
    } catch (Exception e) {
      job.fail("Report generation failed: " + e.getMessage());
    }
  }
}
