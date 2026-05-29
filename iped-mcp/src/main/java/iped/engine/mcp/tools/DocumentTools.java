package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;

public class DocumentTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;

    public DocumentTools(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(getDocumentMetadata(), getDocumentText(), listCategories());
    }

    private McpServerFeatures.SyncToolSpecification getDocumentMetadata() {
        var props = Map.<String, Object>of(
            "sourceId", Map.of("type", "string", "description", "Source ID"),
            "documentId", Map.of("type", "integer", "description", "Document ID")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("sourceId", "documentId"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_get_document_metadata")
                .description("Get document metadata.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String sourceId = (String) request.arguments().get("sourceId");
                    int docId = ((Number) request.arguments().get("documentId")).intValue();
                    var dto = client.getDocumentMetadata(sourceId, docId);
                    String json = MAPPER.writeValueAsString(dto);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_get_document_metadata", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification getDocumentText() {
        var props = Map.<String, Object>of(
            "sourceId", Map.of("type", "string", "description", "Source ID"),
            "documentId", Map.of("type", "integer", "description", "Document ID")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("sourceId", "documentId"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_get_document_text")
                .description("Get extracted text from document.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String sourceId = (String) request.arguments().get("sourceId");
                    int docId = ((Number) request.arguments().get("documentId")).intValue();
                    String text = client.getDocumentText(sourceId, docId);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(text)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_get_document_text", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification listCategories() {
        var schema = new McpSchema.JsonSchema("object", null, null, null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_list_categories")
                .description("List all evidence categories.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    var result = client.listCategories();
                    String json = MAPPER.writeValueAsString(result.getData());
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_list_categories", e);
                }
            })
            .build();
    }
}
