package iped.engine.webapi;

import iped.data.IIPEDSource;
import iped.datasource.AdditionalStoreHealthRegistry;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reports additional-store counts against the authoritative case item count. */
@Path("v2/cases/{caseId}/additional-index/consistency")
public class AdditionalIndexConsistencyV2 {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("caseId") String caseId) {
        IIPEDSource source = Sources.services().sources().getSourceHandle(caseId);
        if (source == null) return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Case not found")).build();
        Map<String, Integer> stores = AdditionalStoreHealthRegistry.snapshot(caseId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("caseId", caseId);
        result.put("authoritativeItemCount", source.getTotalItems());
        result.put("stores", stores);
        result.put("consistent", stores.values().stream().allMatch(v -> v == source.getTotalItems()));
        return Response.ok(result).build();
    }
}
