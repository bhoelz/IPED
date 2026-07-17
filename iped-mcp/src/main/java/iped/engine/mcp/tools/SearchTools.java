package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.client.WebApiClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tools for full-text search.
 *
 * <ul>
 *   <li>{@code iped_search} — paginated Lucene search with opaque cursor tokens
 * </ul>
 *
 * <h2>Pagination</h2>
 *
 * <p>Responses include a {@code nextCursor} field when more results exist. Pass the cursor back as
 * the {@code cursor} parameter to retrieve the next page without tracking offset/limit arithmetic.
 * Raw {@code offset}/{@code limit} are still accepted for direct access.
 */
public class SearchTools {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final WebApiClient client;
  private final McpAuditLog audit;

  public SearchTools(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
    this.client = client;
    this.audit = audit;
  }

  public List<McpServerFeatures.SyncToolSpecification> specifications() {
    return List.of(search());
  }

  private McpServerFeatures.SyncToolSpecification search() {
    var props =
        Map.<String, Object>of(
            "query",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "Lucene query string. Examples: 'bitcoin', "
                        + "'mediaType:image/*', 'deleted:true AND size:[1048576 TO *]'"),
            "cursor",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "(Optional) Opaque cursor returned as nextCursor in a previous "
                        + "response. When provided, query/offset/limit are ignored."),
            "offset",
                Map.of(
                    "type",
                    "integer",
                    "description",
                    "Zero-based index of the first result (default 0). Ignored when cursor is set."),
            "limit",
                Map.of(
                    "type",
                    "integer",
                    "description",
                    "Max results per page, 1–100 (default 20). Ignored when cursor is set."));
    var schema = new McpSchema.JsonSchema("object", props, null, null, null, null);
    return McpServerFeatures.SyncToolSpecification.builder()
        .tool(
            McpSchema.Tool.builder()
                .name("iped_search")
                .description(
                    """
                                Search the forensic case index using Lucene query syntax.
                                Returns {total, offset, limit, items[], nextCursor?}.
                                Each item has: itemId ("{sourceId}:{docId}"), name, path, mediaType, \
                                size, hash, modDate, categories[].
                                Pass nextCursor back as cursor to fetch the next page without \
                                tracking offsets.
                                Never dump all results at once — always work one page at a time.""")
                .inputSchema(schema)
                .build())
        .callHandler(
            (exchange, request) -> {
              var args = request.arguments();
              String query;
              int offset;
              int limit;

              if (args.containsKey("cursor")
                  && args.get("cursor") != null
                  && !args.get("cursor").toString().isBlank()) {
                try {
                  var decoded = decodeCursor(args.get("cursor").toString());
                  query = decoded.query();
                  offset = decoded.offset();
                  limit = decoded.limit();
                } catch (Exception e) {
                  return CaseTools.err(
                      "iped_search",
                      new IllegalArgumentException("Invalid cursor: " + e.getMessage()));
                }
              } else {
                query = (String) args.get("query");
                offset = args.containsKey("offset") ? ((Number) args.get("offset")).intValue() : 0;
                limit =
                    args.containsKey("limit")
                        ? Math.min(((Number) args.get("limit")).intValue(), 100)
                        : 20;
                if (query == null || query.isBlank()) {
                  return CaseTools.err(
                      "iped_search",
                      new IllegalArgumentException("'query' is required when cursor is not set"));
                }
              }

              try {
                var page = client.search(query, offset, limit);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("total", page.getTotal());
                response.put("offset", page.getOffset());
                response.put("limit", page.getLimit());
                response.put("items", page.getItems());

                int nextOffset = offset + limit;
                if (nextOffset < page.getTotal()) {
                  response.put("nextCursor", encodeCursor(query, nextOffset, limit));
                }

                String json = MAPPER.writeValueAsString(response);
                audit.success(
                    "iped_search", Map.of("query", query, "offset", offset, "limit", limit));
                return CaseTools.ok(json);
              } catch (Exception e) {
                audit.error("iped_search", args, e.getMessage());
                return CaseTools.err("iped_search", e);
              }
            })
        .build();
  }

  // ── Cursor encoding ───────────────────────────────────────────────────────

  private record CursorState(String query, int offset, int limit) {}

  static String encodeCursor(String query, int offset, int limit) throws Exception {
    String json = MAPPER.writeValueAsString(Map.of("q", query, "o", offset, "l", limit));
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  private CursorState decodeCursor(String cursor) throws Exception {
    byte[] bytes = Base64.getUrlDecoder().decode(cursor);
    var node = MAPPER.readTree(bytes);
    String q = node.get("q").asText();
    int o = node.get("o").asInt();
    int l = node.get("l").asInt();
    if (q == null || q.isBlank()) throw new IllegalArgumentException("missing q");
    if (l < 1 || l > 100) throw new IllegalArgumentException("l out of range");
    if (o < 0) throw new IllegalArgumentException("o must be >= 0");
    return new CursorState(q, o, l);
  }
}
