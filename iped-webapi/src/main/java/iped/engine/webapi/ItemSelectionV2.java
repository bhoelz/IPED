package iped.engine.webapi;

import iped.data.IIPEDSource;
import iped.engine.webapi.spi.DocRef;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * v2 per-item selection (checked/unchecked) write endpoints.
 *
 * <p>The existing v1 {@code PUT /selection/add} and {@code PUT /selection/remove}
 * accept batch arrays. These v2 endpoints operate on a single item and follow
 * REST resource semantics (PUT = set, DELETE = unset).
 *
 * <pre>
 * GET    /v2/sources/{sourceId}/items/{id}/selected   — get selection state
 * PUT    /v2/sources/{sourceId}/items/{id}/selected   — mark item as selected
 * DELETE /v2/sources/{sourceId}/items/{id}/selected   — unmark item as selected
 * </pre>
 */
@Path("v2/sources/{sourceId}/items/{id}/selected")
@Produces(MediaType.APPLICATION_JSON)
public class ItemSelectionV2 {

    // ── Get selection state ───────────────────────────────────────────────────

    @GET
    public Response getSelected(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id
    ) {
        IIPEDSource handle = Sources.services().sources().getSourceHandle(sourceId);
        if (handle == null) {
            return sourceNotFound(sourceId);
        }
        boolean selected = handle.getBookmarks().isChecked(id);
        return Response.ok(Map.of("sourceId", sourceId, "itemId", id, "selected", selected)).build();
    }

    // ── Mark as selected ──────────────────────────────────────────────────────

    @PUT
    public Response select(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id
    ) {
        IIPEDSource handle = Sources.services().sources().getSourceHandle(sourceId);
        if (handle == null) {
            return sourceNotFound(sourceId);
        }
        Sources.services().selection().add(List.of(new DocRef(sourceId, id)));
        AuditLogger.log("item.select", Map.of("sourceId", sourceId, "itemId", id));
        return Response.ok(Map.of("selected", true, "sourceId", sourceId, "itemId", id)).build();
    }

    // ── Unmark as selected ────────────────────────────────────────────────────

    @DELETE
    public Response deselect(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id
    ) {
        IIPEDSource handle = Sources.services().sources().getSourceHandle(sourceId);
        if (handle == null) {
            return sourceNotFound(sourceId);
        }
        Sources.services().selection().remove(List.of(new DocRef(sourceId, id)));
        AuditLogger.log("item.deselect", Map.of("sourceId", sourceId, "itemId", id));
        return Response.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Response sourceNotFound(String sourceId) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Source not found: " + sourceId)).build();
    }
}
