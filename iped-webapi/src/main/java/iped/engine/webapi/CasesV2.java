package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.engine.webapi.spi.SourceDescriptor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v2 case management resource.
 *
 * <p>Replaces the v1 {@code /sources} endpoint with case-scoped routes. A "case"
 * in the v2 model is a processed IPED output directory opened as a search source.
 *
 * <h2>Routes</h2>
 * <pre>
 * GET    /v2/cases                  — list all open cases
 * POST   /v2/cases                  — open a case by path
 * DELETE /v2/cases/{id}             — close and unload a case
 * GET    /v2/cases/{id}             — get metadata for one open case
 * </pre>
 *
 * <p>Opening a case is idempotent: if the same {@code id} is already open,
 * {@code 409 Conflict} is returned. The {@code id} is caller-chosen; if omitted,
 * a UUID is generated from the path.
 */
@Api(value = "Cases v2")
@Path("v2/cases")
public class CasesV2 {

    /** Tracks when each case was opened (id → Instant). Survives across requests. */
    private static final ConcurrentHashMap<String, Instant> OPENED_AT = new ConcurrentHashMap<>();

    // ── List ─────────────────────────────────────────────────────────────────

    @ApiOperation("List all open cases")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCases() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SourceDescriptor sd : Sources.services().sources().listSources()) {
            if (AllowedSources.isAllowed(sd.getId())) {
                result.add(buildCaseMap(sd));
            }
        }
        return Response.ok(result).build();
    }

    // ── Open ─────────────────────────────────────────────────────────────────

    @ApiOperation("Open a case by filesystem path")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response openCase(Map<String, String> body) {
        String path = body == null ? null : body.get("path");
        if (path == null || path.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "'path' is required")).build();
        }
        String id = (body.containsKey("id") && body.get("id") != null && !body.get("id").isBlank())
                ? body.get("id")
                : UUID.nameUUIDFromBytes(path.getBytes()).toString();

        try {
            Sources.services().sources().addSource(new SourceDescriptor(id, path));
            OPENED_AT.put(id, Instant.now());
            AuditLogger.log("case.open", Map.of("id", id, "path", path));
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("id", id, "path", path, "status", "open")).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("duplicated id")) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", "Case '" + id + "' is already open")).build();
            }
            return Response.status(422)
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    // ── Get one ──────────────────────────────────────────────────────────────

    @ApiOperation("Get metadata for a single open case")
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCase(@PathParam("id") String id) throws Exception {
        SourceDescriptor sd = Sources.services().sources().getSource(id);
        if (sd == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Case not found: " + id)).build();
        }
        return Response.ok(buildCaseMap(sd)).build();
    }

    // ── Close ────────────────────────────────────────────────────────────────

    @ApiOperation("Close and unload an open case")
    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response closeCase(@PathParam("id") String id) {
        try {
            Sources.services().sources().removeSource(id);
            OPENED_AT.remove(id);
            AuditLogger.log("case.close", Map.of("id", id));
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Case not found: " + id)).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", "Failed to close case: " + e.getMessage())).build();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Map<String, Object> buildCaseMap(SourceDescriptor sd) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", sd.getId());
        m.put("path", sd.getPath());
        m.put("openedAt", OPENED_AT.getOrDefault(sd.getId(), Instant.EPOCH).toString());

        // Item count from the live source handle (best-effort)
        try {
            IIPEDSource handle = Sources.services().sources().getSourceHandle(sd.getId());
            if (handle != null) {
                m.put("itemCount", handle.getTotalItems());
            }
        } catch (Exception ignored) {}

        return m;
    }
}
