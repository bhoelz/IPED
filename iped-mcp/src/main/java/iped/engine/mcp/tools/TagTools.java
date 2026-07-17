package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.client.WebApiClient;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP tools for item tagging (backed by the bookmark API).
 *
 * <p>Tags are implemented as named bookmarks. {@code iped_item_tag} auto-creates the tag bookmark
 * on first use. Both tools require the {@code BOOKMARKS} capability.
 *
 * <ul>
 *   <li>{@code iped_item_tag} — add an item to a named tag
 *   <li>{@code iped_item_untag} — remove an item from a named tag
 * </ul>
 */
public class TagTools {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final WebApiClient client;
  private final McpAuditLog audit;

  public TagTools(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
    this.client = client;
    this.audit = audit;
  }

  public List<McpServerFeatures.SyncToolSpecification> specifications() {
    return List.of(itemTag(), itemUntag());
  }

  // ── iped_item_tag ─────────────────────────────────────────────────────────

  private McpServerFeatures.SyncToolSpecification itemTag() {
    var props =
        Map.<String, Object>of(
            "itemId",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "Composite item ID \"{sourceId}:{docId}\" from iped_search"),
            "tag",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "Tag name to apply. The tag is created automatically if it does not exist."));
    return spec(
        "iped_item_tag",
        "Add a named tag to a forensic item. Tags are implemented as bookmarks — the tag "
            + "is created automatically if it does not already exist. "
            + "Use tags to mark items of interest (e.g. 'relevant', 'reviewed', 'suspicious').",
        new McpSchema.JsonSchema("object", props, List.of("itemId", "tag"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String itemId = (String) args.get("itemId");
          String tag = (String) args.get("tag");
          try {
            var ref = parseItemId(itemId);
            client.tagItem(ref.sourceId(), ref.docId(), tag);
            audit.success("iped_item_tag", Map.of("itemId", itemId, "tag", tag));
            return CaseTools.ok(
                "{\"tagged\":true,\"itemId\":\"" + itemId + "\",\"tag\":\"" + tag + "\"}");
          } catch (Exception e) {
            audit.error("iped_item_tag", args, e.getMessage());
            return CaseTools.err("iped_item_tag", e);
          }
        });
  }

  // ── iped_item_untag ───────────────────────────────────────────────────────

  private McpServerFeatures.SyncToolSpecification itemUntag() {
    var props =
        Map.<String, Object>of(
            "itemId",
                Map.of("type", "string", "description", "Composite item ID \"{sourceId}:{docId}\""),
            "tag", Map.of("type", "string", "description", "Tag name to remove from the item"));
    return spec(
        "iped_item_untag",
        "Remove a tag from a forensic item. The item itself and the tag bookmark are not deleted.",
        new McpSchema.JsonSchema("object", props, List.of("itemId", "tag"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String itemId = (String) args.get("itemId");
          String tag = (String) args.get("tag");
          try {
            var ref = parseItemId(itemId);
            client.untagItem(ref.sourceId(), ref.docId(), tag);
            audit.success("iped_item_untag", Map.of("itemId", itemId, "tag", tag));
            return CaseTools.ok(
                "{\"untagged\":true,\"itemId\":\"" + itemId + "\",\"tag\":\"" + tag + "\"}");
          } catch (Exception e) {
            audit.error("iped_item_untag", args, e.getMessage());
            return CaseTools.err("iped_item_untag", e);
          }
        });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private record ItemRef(String sourceId, int docId) {}

  private static ItemRef parseItemId(String itemId) {
    if (itemId == null || itemId.isBlank())
      throw new IllegalArgumentException("itemId must not be blank");
    int colon = itemId.lastIndexOf(':');
    if (colon <= 0)
      throw new IllegalArgumentException("itemId must be \"{sourceId}:{docId}\" — got: " + itemId);
    try {
      return new ItemRef(itemId.substring(0, colon), Integer.parseInt(itemId.substring(colon + 1)));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "docId part of itemId must be an integer — got: " + itemId);
    }
  }

  private static McpServerFeatures.SyncToolSpecification spec(
      String name,
      String description,
      McpSchema.JsonSchema schema,
      BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>
          handler) {
    return McpServerFeatures.SyncToolSpecification.builder()
        .tool(
            McpSchema.Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(schema)
                .build())
        .callHandler(handler)
        .build();
  }
}
