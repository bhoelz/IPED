package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.client.WebApiClient;
import iped.engine.mcp.client.dto.DocRefDto;

import java.util.List;
import java.util.Map;

public class SelectionTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;

    public SelectionTools(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(getSelection(), addToSelection(), removeFromSelection());
    }

    private McpServerFeatures.SyncToolSpecification getSelection() {
        var schema = new McpSchema.JsonSchema("object", null, null, null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_list_selection")
                .description("Get all currently selected documents.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    var result = client.getSelection();
                    String json = MAPPER.writeValueAsString(result);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_list_selection", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification addToSelection() {
        var props = Map.<String, Object>of(
            "docs", BookmarkTools.docsArrayProperty("Documents to add to the selection")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("docs"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_add_to_selection")
                .description("Add documents to the current selection.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    DocRefDto[] docs = BookmarkTools.parseDocs(request.arguments().get("docs"));
                    client.addToSelection(docs);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_add_to_selection", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification removeFromSelection() {
        var props = Map.<String, Object>of(
            "docs", BookmarkTools.docsArrayProperty("Documents to remove from the selection")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("docs"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_remove_from_selection")
                .description("Remove documents from the current selection.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    DocRefDto[] docs = BookmarkTools.parseDocs(request.arguments().get("docs"));
                    client.removeFromSelection(docs);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_remove_from_selection", e);
                }
            })
            .build();
    }
}
