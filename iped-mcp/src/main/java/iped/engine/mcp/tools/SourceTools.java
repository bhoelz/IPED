package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;

public class SourceTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;

    public SourceTools(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(listSources(), getSource(), addSource());
    }

    private McpServerFeatures.SyncToolSpecification listSources() {
        var schema = new McpSchema.JsonSchema("object", null, null, null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_list_sources")
                .description("List all forensic data sources loaded in IPED.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    var result = client.listSources();
                    String json = MAPPER.writeValueAsString(result.getData());
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_list_sources", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification getSource() {
        var props = Map.<String, Object>of("sourceId", Map.of("type", "string", "description", "Source ID"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("sourceId"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_get_source")
                .description("Get properties of a data source.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String sourceId = (String) request.arguments().get("sourceId");
                    var dto = client.getSource(sourceId);
                    String json = MAPPER.writeValueAsString(dto);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_get_source", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification addSource() {
        var props = Map.<String, Object>of(
            "id", Map.of("type", "string", "description", "Source ID"),
            "path", Map.of("type", "string", "description", "Source path")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("id", "path"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_add_source")
                .description("Add a new data source to IPED.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String id = (String) request.arguments().get("id");
                    String path = (String) request.arguments().get("path");
                    client.addSource(id, path);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_add_source", e);
                }
            })
            .build();
    }
}
