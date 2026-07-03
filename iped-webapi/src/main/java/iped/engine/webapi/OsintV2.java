package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.osint.EngineOsintServices;
import iped.osint.spi.OsintExecutionMode;
import iped.osint.spi.OsintIndicatorType;
import iped.osint.spi.OsintPluginException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Api(value = "OSINT")
@Path("v2/osint")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OsintV2 {

    @GET
    @Path("plugins")
    @ApiOperation("List OSINT plugins available to the current engine")
    public Response plugins() throws IOException {
        var source = firstSource();
        return Response.ok(Map.of("plugins", EngineOsintServices.forSource(source).listPlugins())).build();
    }

    @GET
    @Path("plugins/{pluginId}")
    @ApiOperation("Get a single OSINT plugin descriptor")
    public Response plugin(@PathParam("pluginId") String pluginId) throws IOException {
        var source = firstSource();
        return EngineOsintServices.forSource(source).getPlugin(pluginId)
                .<Response>map(descriptor -> Response.ok(descriptor).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Plugin not found: " + pluginId))
                        .build());
    }

    @POST
    @Path("search")
    @ApiOperation("Execute an OSINT search for an explicit indicator or for an item")
    public Response search(Map<String, Object> body) throws Exception {
        String sourceId = required(body, "sourceId");
        var source = Sources.getSource(sourceId);
        var service = EngineOsintServices.forSource(source);
        Integer itemId = body.get("itemId") instanceof Number n ? n.intValue() : null;
        String pluginId = string(body.get("pluginId"));
        List<String> pluginIds = body.get("pluginIds") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> options = body.get("options") instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map)
                : Map.of();

        try {
            if (body.containsKey("indicatorType") && body.containsKey("value")) {
                var result = service.execute(
                        pluginId,
                        OsintIndicatorType.valueOf(required(body, "indicatorType").toUpperCase()),
                        required(body, "value"),
                        itemId,
                        sourceId,
                        OsintExecutionMode.MCP,
                        options);
                AuditLogger.log("osint.search", Map.of("sourceId", sourceId, "pluginId", pluginId == null ? "" : pluginId));
                return Response.ok(result).build();
            }
            if (itemId == null) {
                throw new BadRequestException("itemId is required when indicatorType/value are not provided");
            }
            var item = source.getItemByID(itemId);
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Item not found: " + itemId)).build();
            }
            var results = service.searchItem(itemId, sourceId, OsintExecutionMode.MCP, pluginIds, options, item,
                    EngineOsintServices.extractor());
            AuditLogger.log("osint.search-item", Map.of("sourceId", sourceId, "itemId", itemId, "pluginCount", pluginIds.size()));
            return Response.ok(Map.of("sourceId", sourceId, "itemId", itemId, "results", results, "summary", service.summarize(results))).build();
        } catch (OsintPluginException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("results")
    @ApiOperation("List persisted OSINT results")
    public Response results(@QueryParam("sourceId") String sourceId,
                            @QueryParam("itemId") Integer itemId,
                            @QueryParam("pluginId") String pluginId,
                            @DefaultValue("20") @QueryParam("limit") int limit) throws IOException {
        var source = sourceId == null || sourceId.isBlank() ? firstSource() : Sources.getSource(sourceId);
        String effectiveSourceId = sourceId == null || sourceId.isBlank()
                ? Sources.services().sources().getSourceStringId(source.getSourceId())
                : sourceId;
        return Response.ok(Map.of("results", EngineOsintServices.forSource(source)
                .listResults(effectiveSourceId, itemId, pluginId, limit))).build();
    }

    @GET
    @Path("results/{executionId}")
    @ApiOperation("Get one persisted OSINT result")
    public Response result(@PathParam("executionId") String executionId,
                           @QueryParam("sourceId") String sourceId) throws IOException {
        var source = sourceId == null || sourceId.isBlank() ? firstSource() : Sources.getSource(sourceId);
        return EngineOsintServices.forSource(source).getResult(executionId)
                .<Response>map(result -> Response.ok(result).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "OSINT result not found: " + executionId))
                        .build());
    }

    private static iped.data.IIPEDSource firstSource() {
        try {
            var sources = Sources.services().sources().listSources();
            if (sources.isEmpty()) {
                throw new NotFoundException("No open case available");
            }
            return Sources.getSource(sources.get(0).getId());
        } catch (Exception e) {
            throw new NotFoundException("No open case available");
        }
    }

    private static String required(Map<String, Object> body, String key) {
        String value = string(body.get(key));
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Missing required field: " + key);
        }
        return value;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
