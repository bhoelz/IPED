package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;

public class CaseTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;

    public CaseTools(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(
            listCases(),
            getCaseStatus(),
            pauseCase(),
            resumeCase()
        );
    }

    private McpServerFeatures.SyncToolSpecification listCases() {
        var schema = new McpSchema.JsonSchema("object", null, null, null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_list_cases")
                .description("List all active forensic case UUIDs")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    var result = client.listCases();
                    String json = MAPPER.writeValueAsString(result.getData());
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return errorResult("iped_list_cases", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification getCaseStatus() {
        var props = Map.<String, Object>of("caseId", Map.of("type", "string", "description", "Case UUID"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("caseId"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_get_case_status")
                .description("Get case status")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String caseId = (String) request.arguments().get("caseId");
                    var dto = client.getCaseStatus(caseId);
                    String json = MAPPER.writeValueAsString(dto);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return errorResult("iped_get_case_status", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification pauseCase() {
        var props = Map.<String, Object>of("caseId", Map.of("type", "string", "description", "Case UUID"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("caseId"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_pause_case")
                .description("Pause case processing")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String caseId = (String) request.arguments().get("caseId");
                    client.pauseCase(caseId);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return errorResult("iped_pause_case", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification resumeCase() {
        var props = Map.<String, Object>of("caseId", Map.of("type", "string", "description", "Case UUID"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("caseId"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_resume_case")
                .description("Resume case processing")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String caseId = (String) request.arguments().get("caseId");
                    client.resumeCase(caseId);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return errorResult("iped_resume_case", e);
                }
            })
            .build();
    }

    static McpSchema.CallToolResult errorResult(String toolName, Exception e) {
        String msg = "Tool " + toolName + " failed: " + e.getMessage();
        return McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(msg)))
            .isError(true)
            .build();
    }
}
