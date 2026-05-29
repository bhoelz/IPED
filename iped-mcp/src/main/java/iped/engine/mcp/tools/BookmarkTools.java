package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.client.WebApiClient;
import iped.engine.mcp.client.dto.DocRefDto;

import java.util.List;
import java.util.Map;

public class BookmarkTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;

    public BookmarkTools(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(
            listBookmarks(),
            getBookmarkDocs(),
            createBookmark(),
            deleteBookmark(),
            addDocsToBookmark(),
            removeDocsFromBookmark(),
            renameBookmark()
        );
    }

    private McpServerFeatures.SyncToolSpecification listBookmarks() {
        var schema = new McpSchema.JsonSchema("object", null, null, null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_list_bookmarks")
                .description("List all bookmarks.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    var result = client.listBookmarks();
                    String json = MAPPER.writeValueAsString(result.getData());
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_list_bookmarks", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification getBookmarkDocs() {
        var props = Map.<String, Object>of("name", Map.of("type", "string", "description", "Bookmark name"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("name"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_get_bookmark_docs")
                .description("Get documents in a bookmark.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String name = (String) request.arguments().get("name");
                    var result = client.getBookmarkDocs(name);
                    String json = MAPPER.writeValueAsString(result);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(json)))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_get_bookmark_docs", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification createBookmark() {
        var props = Map.<String, Object>of("name", Map.of("type", "string", "description", "Bookmark name"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("name"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_create_bookmark")
                .description("Create a new bookmark.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String name = (String) request.arguments().get("name");
                    client.createBookmark(name);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_create_bookmark", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification deleteBookmark() {
        var props = Map.<String, Object>of("name", Map.of("type", "string", "description", "Bookmark name"));
        var schema = new McpSchema.JsonSchema("object", props, List.of("name"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_delete_bookmark")
                .description("Delete a bookmark.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String name = (String) request.arguments().get("name");
                    client.deleteBookmark(name);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_delete_bookmark", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification addDocsToBookmark() {
        var props = Map.<String, Object>of(
            "name", Map.of("type", "string", "description", "Bookmark name"),
            "docs", docsArrayProperty("Documents to add to the bookmark")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("name", "docs"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_add_docs_to_bookmark")
                .description("Add documents to an existing bookmark.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String name = (String) request.arguments().get("name");
                    DocRefDto[] docs = parseDocs(request.arguments().get("docs"));
                    client.addDocsToBookmark(name, docs);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_add_docs_to_bookmark", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification removeDocsFromBookmark() {
        var props = Map.<String, Object>of(
            "name", Map.of("type", "string", "description", "Bookmark name"),
            "docs", docsArrayProperty("Documents to remove from the bookmark")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("name", "docs"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_remove_docs_from_bookmark")
                .description("Remove documents from an existing bookmark.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String name = (String) request.arguments().get("name");
                    DocRefDto[] docs = parseDocs(request.arguments().get("docs"));
                    client.removeDocsFromBookmark(name, docs);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_remove_docs_from_bookmark", e);
                }
            })
            .build();
    }

    private McpServerFeatures.SyncToolSpecification renameBookmark() {
        var props = Map.<String, Object>of(
            "oldName", Map.of("type", "string", "description", "Current bookmark name"),
            "newName", Map.of("type", "string", "description", "New bookmark name")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("oldName", "newName"), null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder()
                .name("iped_rename_bookmark")
                .description("Rename an existing bookmark.")
                .inputSchema(schema)
                .build())
            .callHandler((exchange, request) -> {
                try {
                    String oldName = (String) request.arguments().get("oldName");
                    String newName = (String) request.arguments().get("newName");
                    client.renameBookmark(oldName, newName);
                    return McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent("ok")))
                        .build();
                } catch (Exception e) {
                    return CaseTools.errorResult("iped_rename_bookmark", e);
                }
            })
            .build();
    }

    /** JSON-schema fragment describing an array of {source, id} document references. */
    static Map<String, Object> docsArrayProperty(String description) {
        return Map.of(
            "type", "array",
            "description", description,
            "items", Map.of(
                "type", "object",
                "properties", Map.of(
                    "source", Map.of("type", "string", "description", "Source ID"),
                    "id", Map.of("type", "integer", "description", "Document ID")
                ),
                "required", List.of("source", "id")
            )
        );
    }

    /** Convert the MCP "docs" argument (a List of {source, id} maps) into DocRefDto[]. */
    static DocRefDto[] parseDocs(Object docsArg) {
        if (!(docsArg instanceof List<?> list)) {
            throw new IllegalArgumentException("'docs' must be an array of {source, id} objects");
        }
        DocRefDto[] result = new DocRefDto[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Map<?, ?> m)) {
                throw new IllegalArgumentException("each doc must be an object with 'source' and 'id'");
            }
            String source = (String) m.get("source");
            int id = ((Number) m.get("id")).intValue();
            result[i] = new DocRefDto(source, id);
        }
        return result;
    }
}
