package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * MCP tools for case lifecycle management.
 *
 * <ul>
 *   <li>{@code iped_case_list}   — list open cases (filtered to allowlist when set)</li>
 *   <li>{@code iped_case_open}   — open (mount) a case by file-system path</li>
 *   <li>{@code iped_case_get}    — get details for a single case</li>
 *   <li>{@code iped_case_close}  — close (unmount) a case</li>
 * </ul>
 */
public class CaseTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;
    private final McpAuditLog audit;
    private final McpSessionContext session;

    public CaseTools(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
        this.client  = client;
        this.audit   = audit;
        this.session = session;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(caseList(), caseOpen(), caseGet(), caseClose());
    }

    // ── iped_case_list ────────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification caseList() {
        return spec("iped_case_list",
                "List all currently open forensic cases. Returns an array of case objects "
                + "each with an id, path, totalItems count, and openedAt timestamp. "
                + "Call this first to discover which cases are available before searching.",
                new McpSchema.JsonSchema("object", null, null, null, null, null),
                (exchange, request) -> {
                    try {
                        List<Map<String, Object>> cases = client.listCases();
                        if (session.hasCaseRestriction()) {
                            cases = cases.stream()
                                    .filter(c -> session.allowedCases.contains(
                                            String.valueOf(c.get("id"))))
                                    .collect(Collectors.toList());
                        }
                        String json = MAPPER.writeValueAsString(cases);
                        audit.success("iped_case_list", Map.of());
                        return ok(json);
                    } catch (Exception e) {
                        audit.error("iped_case_list", Map.of(), e.getMessage());
                        return err("iped_case_list", e);
                    }
                });
    }

    // ── iped_case_open ────────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification caseOpen() {
        var props = Map.<String, Object>of(
                "path", Map.of("type", "string",
                        "description", "Absolute file-system path to a processed IPED case directory"),
                "id",   Map.of("type", "string",
                        "description", "(Optional) Caller-chosen case ID. A UUID is derived from the path when omitted.")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("path"), null, null, null);
        return spec("iped_case_open",
                "Open (mount) a forensic case from the engine's file system. "
                + "Returns the new case's id and totalItems count. "
                + "Returns an error when a case with the same id is already open (409).",
                schema,
                (exchange, request) -> {
                    var args = request.arguments();
                    String path = (String) args.get("path");
                    String id   = args.containsKey("id") ? (String) args.get("id") : null;
                    if (id != null) {
                        try {
                            session.check(id);
                        } catch (McpSessionContext.CaseAccessDeniedException e) {
                            audit.error("iped_case_open", args, e.getMessage());
                            return err("iped_case_open", e);
                        }
                    }
                    try {
                        var result = client.openCase(id, path);
                        String json = MAPPER.writeValueAsString(result);
                        audit.success("iped_case_open", args);
                        return ok(json);
                    } catch (Exception e) {
                        audit.error("iped_case_open", args, e.getMessage());
                        return err("iped_case_open", e);
                    }
                });
    }

    // ── iped_case_get ─────────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification caseGet() {
        var props = Map.<String, Object>of(
                "caseId", Map.of("type", "string",
                        "description", "Case ID returned by iped_case_list or iped_case_open")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("caseId"), null, null, null);
        return spec("iped_case_get",
                "Get details for a single open case by its ID. Returns id, path, totalItems, openedAt.",
                schema,
                (exchange, request) -> {
                    var args = request.arguments();
                    String caseId = (String) args.get("caseId");
                    try {
                        session.check(caseId);
                        var result = client.getCase(caseId);
                        String json = MAPPER.writeValueAsString(result);
                        audit.success("iped_case_get", args);
                        return ok(json);
                    } catch (McpSessionContext.CaseAccessDeniedException e) {
                        audit.error("iped_case_get", args, e.getMessage());
                        return err("iped_case_get", e);
                    } catch (Exception e) {
                        audit.error("iped_case_get", args, e.getMessage());
                        return err("iped_case_get", e);
                    }
                });
    }

    // ── iped_case_close ───────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification caseClose() {
        var props = Map.<String, Object>of(
                "caseId", Map.of("type", "string", "description", "Case ID to close")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("caseId"), null, null, null);
        return spec("iped_case_close",
                "Close (unmount) an open case. The case data on disk is not modified.",
                schema,
                (exchange, request) -> {
                    var args = request.arguments();
                    String caseId = (String) args.get("caseId");
                    try {
                        session.check(caseId);
                        client.closeCase(caseId);
                        audit.success("iped_case_close", args);
                        return ok("{\"closed\":true,\"caseId\":\"" + caseId + "\"}");
                    } catch (McpSessionContext.CaseAccessDeniedException e) {
                        audit.error("iped_case_close", args, e.getMessage());
                        return err("iped_case_close", e);
                    } catch (Exception e) {
                        audit.error("iped_case_close", args, e.getMessage());
                        return err("iped_case_close", e);
                    }
                });
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    static McpSchema.CallToolResult ok(String text) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(text)))
                .build();
    }

    static McpSchema.CallToolResult err(String toolName, Exception e) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(
                        "Tool " + toolName + " failed: " + e.getMessage())))
                .isError(true)
                .build();
    }

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
