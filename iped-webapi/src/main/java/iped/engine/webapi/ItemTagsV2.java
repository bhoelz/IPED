package iped.engine.webapi;

import iped.data.IIPEDSource;
import iped.engine.webapi.spi.DocRef;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * v2 item-level tag endpoints.
 *
 * <p>Tags are bookmark-backed: each tag name corresponds to a named bookmark that is
 * auto-created on first use. This mirrors the semantics exposed by
 * {@code iped_item_tag} / {@code iped_item_untag} in iped-mcp, providing a
 * REST-ergonomic alternative to the bookmark membership endpoints.
 *
 * <pre>
 * GET    /v2/sources/{sourceId}/items/{id}/tags          — list tags applied to item
 * PUT    /v2/sources/{sourceId}/items/{id}/tags/{tag}    — add tag (creates bookmark on first use)
 * DELETE /v2/sources/{sourceId}/items/{id}/tags/{tag}    — remove tag (204 when tag absent)
 * </pre>
 */
@Path("v2/sources/{sourceId}/items/{id}/tags")
@Produces(MediaType.APPLICATION_JSON)
public class ItemTagsV2 {

    // ── List tags on an item ──────────────────────────────────────────────────

    @GET
    public Response listTags(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id
    ) {
        IIPEDSource handle = Sources.services().sources().getSourceHandle(sourceId);
        if (handle == null) {
            return sourceNotFound(sourceId);
        }
        List<String> tags = handle.getBookmarks().getBookmarkList(id);
        return Response.ok(Map.of("sourceId", sourceId, "itemId", id, "tags",
                tags != null ? tags : List.of())).build();
    }

    // ── Add tag ───────────────────────────────────────────────────────────────

    @PUT
    @Path("{tag}")
    public Response addTag(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id,
            @PathParam("tag") String tag
    ) {
        if (tag == null || tag.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tag name must not be blank")).build();
        }
        IIPEDSource handle = Sources.services().sources().getSourceHandle(sourceId);
        if (handle == null) {
            return sourceNotFound(sourceId);
        }

        ensureBookmarkExists(tag);
        Sources.services().bookmarks().add(tag, List.of(new DocRef(sourceId, id)));
        AuditLogger.log("item.tag", Map.of("sourceId", sourceId, "itemId", id, "tag", tag));
        return Response.ok(Map.of("tagged", true, "sourceId", sourceId, "itemId", id, "tag", tag))
                .build();
    }

    // ── Remove tag ────────────────────────────────────────────────────────────

    @DELETE
    @Path("{tag}")
    public Response removeTag(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id,
            @PathParam("tag") String tag
    ) {
        if (tag == null || tag.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "tag name must not be blank")).build();
        }
        IIPEDSource handle = Sources.services().sources().getSourceHandle(sourceId);
        if (handle == null) {
            return sourceNotFound(sourceId);
        }

        // Silently succeed when the bookmark (tag) does not exist
        if (!Sources.services().bookmarks().listBookmarks().contains(tag)) {
            return Response.noContent().build();
        }
        Sources.services().bookmarks().remove(tag, List.of(new DocRef(sourceId, id)));
        AuditLogger.log("item.untag", Map.of("sourceId", sourceId, "itemId", id, "tag", tag));
        return Response.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void ensureBookmarkExists(String name) {
        if (!Sources.services().bookmarks().listBookmarks().contains(name)) {
            try {
                Sources.services().bookmarks().create(name);
            } catch (Exception ignored) {
                // concurrent create — bookmark now exists, proceed
            }
        }
    }

    private static Response sourceNotFound(String sourceId) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Source not found: " + sourceId)).build();
    }
}
