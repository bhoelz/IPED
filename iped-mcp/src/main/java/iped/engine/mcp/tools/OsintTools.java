package iped.engine.mcp.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class OsintTools {

    private final WebApiClient client;
    private final McpAuditLog audit;
    private final McpSessionContext session;

    public OsintTools(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
        this.client = client;
        this.audit = audit;
        this.session = session;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(pluginList(), pluginGet(), search(), resultList(), resultGet());
    }

    private McpServerFeatures.SyncToolSpecification pluginList() {
        return spec("iped_osint_plugin_list",
                "List OSINT plugins available in the engine.",
                new McpSchema.JsonSchema("object", null, null, null, null, null),
                (exchange, request) -> {
                    try {
                        audit.success("iped_osint_plugin_list", Map.of());
                        return CaseTools.ok(Jsons.toJson(Map.of("plugins", client.listOsintPlugins())));
                    } catch (Exception e) {
                        audit.error("iped_osint_plugin_list", Map.of(), e.getMessage());
                        return CaseTools.err("iped_osint_plugin_list", e);
                    }
                });
    }

    private McpServerFeatures.SyncToolSpecification pluginGet() {
        var props = Map.<String, Object>of("pluginId", Map.of("type", "string"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("pluginId"), null, null, null);
        return spec("iped_osint_plugin_get",
                "Get a single OSINT plugin descriptor.",
                schema,
                (exchange, request) -> {
                    var args = request.arguments();
                    try {
                        audit.success("iped_osint_plugin_get", args);
                        return CaseTools.ok(Jsons.toJson(client.getOsintPlugin(String.valueOf(args.get("pluginId")))));
                    } catch (Exception e) {
                        audit.error("iped_osint_plugin_get", args, e.getMessage());
                        return CaseTools.err("iped_osint_plugin_get", e);
                    }
                });
    }

    private McpServerFeatures.SyncToolSpecification search() {
        var props = Map.<String, Object>of(
                "sourceId", Map.of("type", "string"),
                "itemId", Map.of("type", "integer"),
                "pluginId", Map.of("type", "string"),
                "pluginIds", Map.of("type", "array"),
                "indicatorType", Map.of("type", "string"),
                "value", Map.of("type", "string"),
                "options", Map.of("type", "object"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("sourceId"), null, null, null);
        return spec("iped_osint_search",
                "Execute an OSINT query for an explicit indicator or for a specific item.",
                schema,
                (exchange, request) -> {
                    var args = request.arguments();
                    try {
                        String sourceId = (String) args.get("sourceId");
                        if (sourceId != null) {
                            session.check(sourceId);
                        }
                        audit.success("iped_osint_search", args);
                        return CaseTools.ok(Jsons.toJson(client.searchOsint(args)));
                    } catch (Exception e) {
                        audit.error("iped_osint_search", args, e.getMessage());
                        return CaseTools.err("iped_osint_search", e);
                    }
                });
    }

    private McpServerFeatures.SyncToolSpecification resultList() {
        var props = Map.<String, Object>of(
                "sourceId", Map.of("type", "string"),
                "itemId", Map.of("type", "integer"),
                "pluginId", Map.of("type", "string"),
                "limit", Map.of("type", "integer"));
        var schema = new McpSchema.JsonSchema("object", props, null, null, null, null);
        return spec("iped_osint_result_list",
                "List persisted OSINT results for a case or item.",
                schema,
                (exchange, request) -> {
                    var args = request.arguments();
                    try {
                        String sourceId = args.get("sourceId") == null ? null : String.valueOf(args.get("sourceId"));
                        if (sourceId != null) {
                            session.check(sourceId);
                        }
                        Integer itemId = args.get("itemId") instanceof Number n ? n.intValue() : null;
                        String pluginId = args.get("pluginId") == null ? null : String.valueOf(args.get("pluginId"));
                        int limit = args.get("limit") instanceof Number n ? n.intValue() : 20;
                        audit.success("iped_osint_result_list", args);
                        return CaseTools.ok(Jsons.toJson(client.listOsintResults(sourceId, itemId, pluginId, limit)));
                    } catch (Exception e) {
                        audit.error("iped_osint_result_list", args, e.getMessage());
                        return CaseTools.err("iped_osint_result_list", e);
                    }
                });
    }

    private McpServerFeatures.SyncToolSpecification resultGet() {
        var props = Map.<String, Object>of(
                "executionId", Map.of("type", "string"),
                "sourceId", Map.of("type", "string"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("executionId"), null, null, null);
        return spec("iped_osint_result_get",
                "Get a persisted OSINT result by execution id.",
                schema,
                (exchange, request) -> {
                    var args = request.arguments();
                    try {
                        String sourceId = args.get("sourceId") == null ? null : String.valueOf(args.get("sourceId"));
                        if (sourceId != null) {
                            session.check(sourceId);
                        }
                        audit.success("iped_osint_result_get", args);
                        return CaseTools.ok(Jsons.toJson(client.getOsintResult(sourceId, String.valueOf(args.get("executionId")))));
                    } catch (Exception e) {
                        audit.error("iped_osint_result_get", args, e.getMessage());
                        return CaseTools.err("iped_osint_result_get", e);
                    }
                });
    }

    private static McpServerFeatures.SyncToolSpecification spec(
            String name, String description, McpSchema.JsonSchema schema,
            BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder().name(name).description(description).inputSchema(schema).build())
                .callHandler(handler)
                .build();
    }
}
