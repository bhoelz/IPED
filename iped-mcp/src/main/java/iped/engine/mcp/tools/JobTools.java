package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.client.WebApiClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP tools for async job management.
 *
 * <p>Jobs represent long-running operations (export, report) backed by the
 * {@code POST/GET/DELETE /v2/jobs} endpoints in iped-webapi. All tools require
 * the {@code JOBS} capability.
 *
 * <ul>
 *   <li>{@code iped_job_export}  — submit an export job for a set of items</li>
 *   <li>{@code iped_job_status}  — poll a job's current status and progress</li>
 *   <li>{@code iped_job_cancel}  — cancel a pending or running job</li>
 * </ul>
 */
public class JobTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;
    private final McpAuditLog audit;

    public JobTools(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
        this.client = client;
        this.audit  = audit;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(jobExport(), jobStatus(), jobCancel());
    }

    // ── iped_job_export ───────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification jobExport() {
        var props = Map.<String, Object>of(
                "sourceId",    Map.of("type", "string",
                        "description", "Source ID owning the items to export"),
                "itemIds",     Map.of("type", "array",
                        "description", "List of integer docIds to include in the export",
                        "items", Map.of("type", "integer")),
                "outputPath",  Map.of("type", "string",
                        "description", "(Optional) Absolute path for the output ZIP file. " +
                                "Defaults to a temp-dir path on the engine host.")
        );
        return spec("iped_job_export",
                """
                Submit an async export job that packs forensic items into a ZIP file.
                Returns immediately with a jobId — use iped_job_status to poll progress.
                The export runs on the engine host; outputPath (when provided) must be
                writable on that host. Typical flow:
                  1. Collect itemIds from iped_search results.
                  2. Call iped_job_export to start the ZIP.
                  3. Poll iped_job_status until status is "completed" or "failed".
                  4. The completed message contains the output path.""",
                new McpSchema.JsonSchema("object", props, List.of("sourceId", "itemIds"),
                        null, null, null),
                (exchange, request) -> {
                    var args = request.arguments();
                    String sourceId = (String) args.get("sourceId");
                    Object rawIds   = args.get("itemIds");

                    if (!(rawIds instanceof List<?> idList) || idList.isEmpty()) {
                        return CaseTools.err("iped_job_export",
                                new IllegalArgumentException("'itemIds' must be a non-empty array of integers"));
                    }
                    List<Integer> itemIds = idList.stream()
                            .map(o -> ((Number) o).intValue())
                            .toList();

                    Map<String, Object> params = new HashMap<>();
                    params.put("sourceId", sourceId);
                    params.put("itemIds",  itemIds);
                    if (args.containsKey("outputPath") && args.get("outputPath") != null) {
                        params.put("outputPath", args.get("outputPath"));
                    }

                    try {
                        var result = client.submitJob("export", params);
                        String json = MAPPER.writeValueAsString(result);
                        audit.success("iped_job_export",
                                Map.of("sourceId", sourceId, "itemCount", itemIds.size()));
                        return CaseTools.ok(json);
                    } catch (Exception e) {
                        audit.error("iped_job_export",
                                Map.of("sourceId", sourceId, "itemCount", itemIds.size()),
                                e.getMessage());
                        return CaseTools.err("iped_job_export", e);
                    }
                });
    }

    // ── iped_job_status ───────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification jobStatus() {
        var props = Map.<String, Object>of(
                "jobId", Map.of("type", "string",
                        "description", "Job ID returned by iped_job_export")
        );
        return spec("iped_job_status",
                """
                Poll the status of an async job.
                Returns: id, type, status (pending|running|completed|failed|cancelled),
                progress (0–100), message, createdAt, and terminatedAt when done.
                When status is "completed", message contains the output path.""",
                new McpSchema.JsonSchema("object", props, List.of("jobId"), null, null, null),
                (exchange, request) -> {
                    var args = request.arguments();
                    String jobId = (String) args.get("jobId");
                    try {
                        var dto = client.getJob(jobId);
                        String json = MAPPER.writeValueAsString(dto);
                        audit.success("iped_job_status", args);
                        return CaseTools.ok(json);
                    } catch (Exception e) {
                        audit.error("iped_job_status", args, e.getMessage());
                        return CaseTools.err("iped_job_status", e);
                    }
                });
    }

    // ── iped_job_cancel ───────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification jobCancel() {
        var props = Map.<String, Object>of(
                "jobId", Map.of("type", "string",
                        "description", "Job ID of the pending or running job to cancel")
        );
        return spec("iped_job_cancel",
                "Cancel a pending or running job. Returns 409-conflict when the job is already " +
                "in a terminal state (completed, failed, or cancelled). " +
                "Cancelled jobs are not retried automatically.",
                new McpSchema.JsonSchema("object", props, List.of("jobId"), null, null, null),
                (exchange, request) -> {
                    var args = request.arguments();
                    String jobId = (String) args.get("jobId");
                    try {
                        client.cancelJob(jobId);
                        audit.success("iped_job_cancel", args);
                        return CaseTools.ok("{\"cancelled\":true,\"jobId\":\"" + jobId + "\"}");
                    } catch (Exception e) {
                        audit.error("iped_job_cancel", args, e.getMessage());
                        return CaseTools.err("iped_job_cancel", e);
                    }
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static McpServerFeatures.SyncToolSpecification spec(
            String name, String description,
            McpSchema.JsonSchema schema,
            BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name(name)
                        .description(description)
                        .inputSchema(schema)
                        .build())
                .callHandler(handler)
                .build();
    }
}
