package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;

public class SearchTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;

    public SearchTools(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(search());
    }

    private McpServerFeatures.SyncToolSpecification search() {
        var props = Map.<String, Object>of(
            "query", Map.of("type", "string", "description", "Lucene query"),
            "sourceId", Map.of("type", "string", "description", "Source ID (optional)")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("query"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_search")
                .description("Search documents using Lucene query syntax.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String query = (String) request.arguments().get("query");
                    Object sourceObj = request.arguments().get("sourceId");
                    String sourceId = sourceObj != null ? sourceObj.toString() : "";
                    var result = client.search(query, sourceId);
                    String json = MAPPER.writeValueAsString(result);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_search", e);
                }
            })
            .build();
    }
}
