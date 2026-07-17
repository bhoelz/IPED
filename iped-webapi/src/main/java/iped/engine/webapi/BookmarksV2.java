package iped.engine.webapi;

import iped.engine.webapi.spi.DocRef;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * v2 bookmark management endpoints.
 *
 * <p>Wraps the existing {@link iped.engine.webapi.spi.BookmarkService} SPI with consistent JSON
 * shapes, proper HTTP status codes, and audit logging.
 *
 * <p>Bookmark names are URL-path-encoded; callers must percent-encode names containing slashes or
 * other reserved characters.
 *
 * <pre>
 * GET  /v2/bookmarks                    — list bookmark names
 * POST /v2/bookmarks                    — create bookmark  (body: {"name":"…"})
 * GET  /v2/bookmarks/{name}             — get bookmark + item count
 * DELETE /v2/bookmarks/{name}           — delete bookmark
 * PATCH /v2/bookmarks/{name}            — rename           (body: {"name":"…"})
 * GET  /v2/bookmarks/{name}/items       — list item refs
 * PUT  /v2/bookmarks/{name}/items       — add items        (body: [{sourceId,docId}…])
 * DELETE /v2/bookmarks/{name}/items     — remove items     (body: [{sourceId,docId}…])
 * </pre>
 */
@Path("v2/bookmarks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookmarksV2 {

  // -----------------------------------------------------------------------
  // List all bookmarks
  // -----------------------------------------------------------------------

  @GET
  public Response list() {
    Set<String> names = Sources.services().bookmarks().listBookmarks();
    return Response.ok(Map.of("bookmarks", names.stream().sorted().toList())).build();
  }

  // -----------------------------------------------------------------------
  // Create bookmark
  // -----------------------------------------------------------------------

  @POST
  public Response create(Map<String, String> body) {
    String name = body == null ? null : body.get("name");
    if (name == null || name.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", "name is required"))
          .build();
    }
    if (Sources.services().bookmarks().listBookmarks().contains(name)) {
      return Response.status(Response.Status.CONFLICT)
          .entity(Map.of("error", "Bookmark already exists: " + name))
          .build();
    }
    Sources.services().bookmarks().create(name);
    AuditLogger.log("bookmark.create", Map.of("name", name));
    return Response.status(Response.Status.CREATED).entity(Map.of("name", name)).build();
  }

  // -----------------------------------------------------------------------
  // Get bookmark summary
  // -----------------------------------------------------------------------

  @GET
  @Path("{name}")
  public Response get(@PathParam("name") String name) throws Exception {
    if (!Sources.services().bookmarks().listBookmarks().contains(name)) {
      return notFound(name);
    }
    List<DocRef> items = Sources.services().bookmarks().listBookmarkDocs(name);
    return Response.ok(Map.of("name", name, "count", items.size())).build();
  }

  // -----------------------------------------------------------------------
  // Delete bookmark
  // -----------------------------------------------------------------------

  @DELETE
  @Path("{name}")
  public Response delete(@PathParam("name") String name) {
    if (!Sources.services().bookmarks().listBookmarks().contains(name)) {
      return notFound(name);
    }
    Sources.services().bookmarks().delete(name);
    AuditLogger.log("bookmark.delete", Map.of("name", name));
    return Response.noContent().build();
  }

  // -----------------------------------------------------------------------
  // Rename bookmark
  // -----------------------------------------------------------------------

  @PATCH
  @Path("{name}")
  public Response rename(@PathParam("name") String oldName, Map<String, String> body) {
    String newName = body == null ? null : body.get("name");
    if (newName == null || newName.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", "name is required"))
          .build();
    }
    if (!Sources.services().bookmarks().listBookmarks().contains(oldName)) {
      return notFound(oldName);
    }
    Sources.services().bookmarks().rename(oldName, newName);
    AuditLogger.log("bookmark.rename", Map.of("from", oldName, "to", newName));
    return Response.ok(Map.of("name", newName)).build();
  }

  // -----------------------------------------------------------------------
  // List items in a bookmark
  // -----------------------------------------------------------------------

  @GET
  @Path("{name}/items")
  public Response listItems(@PathParam("name") String name) throws Exception {
    if (!Sources.services().bookmarks().listBookmarks().contains(name)) {
      return notFound(name);
    }
    List<Map<String, Object>> items =
        Sources.services().bookmarks().listBookmarkDocs(name).stream()
            .map(r -> Map.<String, Object>of("sourceId", r.getSource(), "docId", r.getId()))
            .toList();
    return Response.ok(Map.of("name", name, "items", items)).build();
  }

  // -----------------------------------------------------------------------
  // Add items to bookmark
  // -----------------------------------------------------------------------

  @PUT
  @Path("{name}/items")
  public Response addItems(@PathParam("name") String name, List<Map<String, Object>> body) {
    if (!Sources.services().bookmarks().listBookmarks().contains(name)) {
      return notFound(name);
    }
    List<DocRef> refs = toRefs(body);
    Sources.services().bookmarks().add(name, refs);
    AuditLogger.log("bookmark.add-items", Map.of("name", name, "count", refs.size()));
    return Response.ok(Map.of("added", refs.size())).build();
  }

  // -----------------------------------------------------------------------
  // Remove items from bookmark
  // -----------------------------------------------------------------------

  @DELETE
  @Path("{name}/items")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeItems(@PathParam("name") String name, List<Map<String, Object>> body) {
    if (!Sources.services().bookmarks().listBookmarks().contains(name)) {
      return notFound(name);
    }
    List<DocRef> refs = toRefs(body);
    Sources.services().bookmarks().remove(name, refs);
    AuditLogger.log("bookmark.remove-items", Map.of("name", name, "count", refs.size()));
    return Response.ok(Map.of("removed", refs.size())).build();
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private static Response notFound(String name) {
    return Response.status(Response.Status.NOT_FOUND)
        .entity(Map.of("error", "Bookmark not found: " + name))
        .build();
  }

  private static List<DocRef> toRefs(List<Map<String, Object>> body) {
    if (body == null) return List.of();
    return body.stream()
        .filter(m -> m.containsKey("sourceId") && m.containsKey("docId"))
        .map(
            m ->
                new DocRef(String.valueOf(m.get("sourceId")), ((Number) m.get("docId")).intValue()))
        .toList();
  }
}
