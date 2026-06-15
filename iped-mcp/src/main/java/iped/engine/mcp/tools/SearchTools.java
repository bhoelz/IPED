package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.client.WebApiClient;

import java.util.List;
import java.util.Map;

/**
 * MCP tools for full-text search.
 *
 * <ul>
 *   <li>{@code iped_search} — paginated Lucene search returning item summaries</li>
 * </ul>
 */
public class SearchTools {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebApiClient client;
    private final McpAuditLog audit;

    public SearchTools(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
        this.client = client;
        this.audit  = audit;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(search());
    }

    private McpServerFeatures.SyncToolSpecification search() {
        var props = Map.<String, Object>of(
                "query",  Map.of("type", "string",
                        "description", "Lucene query string. Examples: 'bitcoin', " +
                                "'mediaType:image/*', 'deleted:true AND size:[1048576 TO *]'"),
                "offset", Map.of("type", "integer", "description",
                        "Zero-based index of the first result to return (default 0)"),
                "limit",  Map.of("type", "integer", "description",
                        "Max results per page, 1–100 (default 20). Use pagination for large sets.")
        );
        var schema = new McpSchema.JsonSchema("object", props, List.of("query"), null, null, null);
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("iped_search")
                        .description("""
                                Search the forensic case index using Lucene query syntax.
                                Returns a paginated page: {total, offset, limit, items[]}.
                                Each item has: itemId ("{sourceId}:{docId}"), name, path, mediaType, \
                                size, hash, modDate, categories[].
                                Use offset + limit to page through large result sets.
                                Never dump all results; always work with one page at a time.""")
                        .inputSchema(schema)
                        .build())
                .callHandler((exchange, request) -> {
                    var args = request.arguments();
                    String query  = (String) args.get("query");
                    int    offset = args.containsKey("offset")
                            ? ((Number) args.get("offset")).intValue() : 0;
                    int    limit  = args.containsKey("limit")
                            ? Math.min(((Number) args.get("limit")).intValue(), 100) : 20;
                    try {
                        var page = client.search(query, offset, limit);
                        String json = MAPPER.writeValueAsString(page);
                        audit.success("iped_search", args);
                        return CaseTools.ok(json);
                    } catch (Exception e) {
                        audit.error("iped_search", args, e.getMessage());
                        return CaseTools.err("iped_search", e);
                    }
                })
                .build();
    }
}
