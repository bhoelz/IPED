package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;

public class StatsTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;

    public StatsTools(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(getGlobalStats(), getCaseStats());
    }

    private McpServerFeatures.SyncToolSpecification getGlobalStats() {
        var schema = new McpSchema.JsonSchema("object", null, null, null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_get_global_stats")
                .description("Get global IPED server statistics.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    var dto = client.getGlobalStats();
                    String json = MAPPER.writeValueAsString(dto);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_get_global_stats", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification getCaseStats() {
        var props = Map.<String, Object>of("caseId", Map.of("type", "string", "description", "Case UUID"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("caseId"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_get_case_stats")
                .description("Get case statistics.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String caseId = (String) request.arguments().get("caseId");
                    var dto = client.getCaseStats(caseId);
                    String json = MAPPER.writeValueAsString(dto);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_get_case_stats", e);
                }
            })
            .build();
    }
}
