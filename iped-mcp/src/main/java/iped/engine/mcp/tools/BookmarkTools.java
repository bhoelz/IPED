package iped.engine.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import iped.engine.mcp.GrantedCapabilities;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.client.WebApiClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP tools for bookmark CRUD via the v2 bookmark endpoints.
 *
 * <ul>
 *   <li>{@code iped_bookmark_list} — list all bookmark names
 *   <li>{@code iped_bookmark_items} — items in a bookmark
 *   <li>{@code iped_bookmark_create} — create a bookmark
 *   <li>{@code iped_bookmark_delete} — delete a bookmark
 *   <li>{@code iped_bookmark_rename} — rename a bookmark
 *   <li>{@code iped_bookmark_add_items} — add items to a bookmark
 *   <li>{@code iped_bookmark_remove_items} — remove items from a bookmark
 * </ul>
 */
public class BookmarkTools {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final WebApiClient client;
  private final McpAuditLog audit;
  private final boolean writeEnabled;

  public BookmarkTools(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
    this.client = client;
    this.audit = audit;
    this.writeEnabled = session.can(GrantedCapabilities.BOOKMARKS);
  }

  /**
   * Returns read-only or read-write specifications depending on the session's {@code BOOKMARKS}
   * capability grant.
   */
  public List<McpServerFeatures.SyncToolSpecification> specifications() {
    List<McpServerFeatures.SyncToolSpecification> specs = new ArrayList<>();
    specs.add(bookmarkList());
    specs.add(bookmarkItems());
    if (writeEnabled) {
      specs.add(bookmarkCreate());
      specs.add(bookmarkDelete());
      specs.add(bookmarkRename());
      specs.add(bookmarkAddItems());
      specs.add(bookmarkRemoveItems());
    }
    return specs;
  }

  private McpServerFeatures.SyncToolSpecification bookmarkList() {
    return spec(
        "iped_bookmark_list",
        "List all bookmark names in the case. Bookmarks group items for review or export.",
        new McpSchema.JsonSchema("object", null, null, null, null, null),
        (exchange, request) -> {
          try {
            var names = client.listBookmarks();
            String json = MAPPER.writeValueAsString(names);
            audit.success("iped_bookmark_list", Map.of());
            return CaseTools.ok(json);
          } catch (Exception e) {
            audit.error("iped_bookmark_list", Map.of(), e.getMessage());
            return CaseTools.err("iped_bookmark_list", e);
          }
        });
  }

  private McpServerFeatures.SyncToolSpecification bookmarkItems() {
    var props =
        Map.<String, Object>of("name", Map.of("type", "string", "description", "Bookmark name"));
    return spec(
        "iped_bookmark_items",
        "Get the list of items ({sourceId, docId} pairs) in a named bookmark.",
        new McpSchema.JsonSchema("object", props, List.of("name"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String name = (String) args.get("name");
          try {
            var result = client.getBookmarkItems(name);
            String json = MAPPER.writeValueAsString(result);
            audit.success("iped_bookmark_items", args);
            return CaseTools.ok(json);
          } catch (Exception e) {
            audit.error("iped_bookmark_items", args, e.getMessage());
            return CaseTools.err("iped_bookmark_items", e);
          }
        });
  }

  private McpServerFeatures.SyncToolSpecification bookmarkCreate() {
    var props =
        Map.<String, Object>of(
            "name",
            Map.of(
                "type",
                "string",
                "description",
                "Unique bookmark name (case-sensitive, no leading/trailing spaces)"));
    return spec(
        "iped_bookmark_create",
        "Create a new empty bookmark. Returns 409-conflict error when a bookmark "
            + "with the same name already exists.",
        new McpSchema.JsonSchema("object", props, List.of("name"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String name = (String) args.get("name");
          try {
            var result = client.createBookmark(name);
            String json = MAPPER.writeValueAsString(result);
            audit.success("iped_bookmark_create", args);
            return CaseTools.ok(json);
          } catch (Exception e) {
            audit.error("iped_bookmark_create", args, e.getMessage());
            return CaseTools.err("iped_bookmark_create", e);
          }
        });
  }

  private McpServerFeatures.SyncToolSpecification bookmarkDelete() {
    var props =
        Map.<String, Object>of(
            "name", Map.of("type", "string", "description", "Bookmark name to delete"));
    return spec(
        "iped_bookmark_delete",
        "Delete a bookmark and all its item associations. The items themselves are not deleted.",
        new McpSchema.JsonSchema("object", props, List.of("name"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String name = (String) args.get("name");
          try {
            client.deleteBookmark(name);
            audit.success("iped_bookmark_delete", args);
            return CaseTools.ok("{\"deleted\":true,\"name\":\"" + name + "\"}");
          } catch (Exception e) {
            audit.error("iped_bookmark_delete", args, e.getMessage());
            return CaseTools.err("iped_bookmark_delete", e);
          }
        });
  }

  private McpServerFeatures.SyncToolSpecification bookmarkRename() {
    var props =
        Map.<String, Object>of(
            "name", Map.of("type", "string", "description", "Current bookmark name"),
            "newName", Map.of("type", "string", "description", "New bookmark name"));
    return spec(
        "iped_bookmark_rename",
        "Rename a bookmark. All item associations are preserved under the new name.",
        new McpSchema.JsonSchema("object", props, List.of("name", "newName"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String name = (String) args.get("name");
          String newName = (String) args.get("newName");
          try {
            client.renameBookmark(name, newName);
            audit.success("iped_bookmark_rename", args);
            return CaseTools.ok(
                "{\"renamed\":true,\"from\":\"" + name + "\",\"to\":\"" + newName + "\"}");
          } catch (Exception e) {
            audit.error("iped_bookmark_rename", args, e.getMessage());
            return CaseTools.err("iped_bookmark_rename", e);
          }
        });
  }

  private McpServerFeatures.SyncToolSpecification bookmarkAddItems() {
    var props =
        Map.<String, Object>of(
            "name", Map.of("type", "string", "description", "Bookmark name"),
            "items", itemsArrayProperty("Items to add (each is a {sourceId, docId} pair)"));
    return spec(
        "iped_bookmark_add_items",
        "Add one or more items to a bookmark. Items are specified as {sourceId, docId} pairs. "
            + "The sourceId and docId can be parsed from an itemId (\"sourceId:docId\").",
        new McpSchema.JsonSchema("object", props, List.of("name", "items"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String name = (String) args.get("name");
          List<Map<String, Object>> items = parseItems(args.get("items"));
          try {
            client.addBookmarkItems(name, items);
            audit.success("iped_bookmark_add_items", Map.of("name", name, "count", items.size()));
            return CaseTools.ok("{\"added\":" + items.size() + "}");
          } catch (Exception e) {
            audit.error("iped_bookmark_add_items", args, e.getMessage());
            return CaseTools.err("iped_bookmark_add_items", e);
          }
        });
  }

  private McpServerFeatures.SyncToolSpecification bookmarkRemoveItems() {
    var props =
        Map.<String, Object>of(
            "name", Map.of("type", "string", "description", "Bookmark name"),
            "items", itemsArrayProperty("Items to remove"));
    return spec(
        "iped_bookmark_remove_items",
        "Remove one or more items from a bookmark. The items themselves are not deleted from the case.",
        new McpSchema.JsonSchema("object", props, List.of("name", "items"), null, null, null),
        (exchange, request) -> {
          var args = request.arguments();
          String name = (String) args.get("name");
          List<Map<String, Object>> items = parseItems(args.get("items"));
          try {
            client.removeBookmarkItems(name, items);
            audit.success(
                "iped_bookmark_remove_items", Map.of("name", name, "count", items.size()));
            return CaseTools.ok("{\"removed\":" + items.size() + "}");
          } catch (Exception e) {
            audit.error("iped_bookmark_remove_items", args, e.getMessage());
            return CaseTools.err("iped_bookmark_remove_items", e);
          }
        });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static Map<String, Object> itemsArrayProperty(String description) {
    return Map.of(
        "type",
        "array",
        "description",
        description,
        "items",
        Map.of(
            "type", "object",
            "properties",
                Map.of(
                    "sourceId", Map.of("type", "string"),
                    "docId", Map.of("type", "integer")),
            "required", List.of("sourceId", "docId")));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> parseItems(Object raw) {
    if (!(raw instanceof List<?> list)) {
      throw new IllegalArgumentException("'items' must be an array of {sourceId, docId}");
    }
    List<Map<String, Object>> result = new ArrayList<>(list.size());
    for (Object entry : list) {
      if (!(entry instanceof Map<?, ?> m)) {
        throw new IllegalArgumentException("each item must be {sourceId, docId}");
      }
      result.add(
          Map.of(
              "sourceId", m.get("sourceId").toString(),
              "docId", ((Number) m.get("docId")).intValue()));
    }
    return result;
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
