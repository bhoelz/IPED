package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.data.IItem;
import iped.engine.webapi.spi.SearchPage;
import iped.engine.webapi.spi.SearchResultItem;
import iped.properties.ExtraProperties;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * v2 GeoJSON endpoint — returns a {@code FeatureCollection} of all items that carry
 * GPS coordinates in the given source.
 *
 * <p>Coordinates are stored during indexing in {@code ExtraProperties.LOCATIONS}
 * ({@code "common:geo:locations"}) as {@code "lat;lon"} pairs (semicolon-separated,
 * multi-valued). Altitude is stored in {@code "common:altitude"}.
 *
 * <h2>Endpoints</h2>
 * <pre>
 * GET /v2/sources/{sourceId}/geo
 *     → GeoJSON FeatureCollection for all geo items (up to {@code limit}, default 50 000)
 *
 * GET /v2/sources/{sourceId}/items/{id}/geo
 *     → GeoJSON FeatureCollection for a single item (may contain multiple features)
 * </pre>
 *
 * <h2>GeoJSON output shape</h2>
 * <pre>
 * {
 *   "type": "FeatureCollection",
 *   "total": 1234,
 *   "truncated": false,
 *   "features": [
 *     {
 *       "type": "Feature",
 *       "id": "case-1:42:0",
 *       "geometry": {"type": "Point", "coordinates": [-43.172, -22.906, 890.0]},
 *       "properties": {
 *         "sourceId": "case-1",
 *         "docId": 42,
 *         "name": "photo.jpg",
 *         "timestamp": "2024-01-15T10:30:00Z"
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
@Api(value = "Geo v2")
@Path("v2/sources/{sourceId}")
public class GeoV2 {

    private static final int DEFAULT_LIMIT = 50_000;
    private static final String ALTITUDE_KEY = ExtraProperties.COMMON_META_PREFIX + "altitude";

    // ── Source-level: all geo items ───────────────────────────────────────────

    @ApiOperation(
            value  = "GeoJSON FeatureCollection for all geolocated items in the source",
            notes  = "Items with multiple location values each contribute multiple GeoJSON features. " +
                     "When the result set exceeds the limit, 'truncated' is true."
    )
    @GET
    @Path("geo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sourceGeo(
            @PathParam("sourceId") String sourceId,
            @QueryParam("limit") @DefaultValue("50000") int limitParam
    ) throws Exception {
        IIPEDSource source = Sources.getSource(sourceId);
        if (source == null) {
            return notFound("Source not found: " + sourceId);
        }

        int limit = Math.max(1, Math.min(limitParam, 500_000));
        String geoQuery = ExtraProperties.LOCATIONS.replace(":", "\\:") + ":*";
        SearchPage page = Sources.services().search().searchPaginated(geoQuery, sourceId, 0, limit);

        StringBuilder sb = startFeatureCollection(page.getTotal(), page.getItems().size() < page.getTotal());
        boolean first = true;
        for (SearchResultItem resultItem : page.getItems()) {
            IItem item = source.getItemByID(resultItem.getId());
            if (item == null) continue;
            Map<String, List<String>> meta = item.getMetadataMap();
            List<String> locations = meta.getOrDefault(ExtraProperties.LOCATIONS, List.of());
            int locIdx = 0;
            for (String loc : locations) {
                String featureId = sourceId + ":" + resultItem.getId() + ":" + locIdx++;
                String json = buildFeature(featureId, sourceId, resultItem.getId(), item.getName(),
                        loc, meta);
                if (json != null) {
                    if (!first) sb.append(',');
                    sb.append(json);
                    first = false;
                }
            }
        }
        sb.append("]}");
        return Response.ok(sb.toString(), MediaType.APPLICATION_JSON).build();
    }

    // ── Item-level: single item's geo features ────────────────────────────────

    @ApiOperation(
            value = "GeoJSON FeatureCollection for a single item",
            notes = "Returns an empty FeatureCollection when the item has no location metadata."
    )
    @GET
    @Path("items/{id}/geo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response itemGeo(
            @PathParam("sourceId") String sourceId,
            @PathParam("id") int id
    ) {
        IIPEDSource source = Sources.getSource(sourceId);
        if (source == null) return notFound("Source not found: " + sourceId);

        IItem item = source.getItemByID(id);
        if (item == null) return notFound("Item not found: " + id);

        Map<String, List<String>> meta = item.getMetadataMap();
        List<String> locations = meta.getOrDefault(ExtraProperties.LOCATIONS, List.of());

        StringBuilder sb = startFeatureCollection(locations.size(), false);
        int locIdx = 0;
        boolean first = true;
        for (String loc : locations) {
            String featureId = sourceId + ":" + id + ":" + locIdx++;
            String json = buildFeature(featureId, sourceId, id, item.getName(), loc, meta);
            if (json != null) {
                if (!first) sb.append(',');
                sb.append(json);
                first = false;
            }
        }
        sb.append("]}");
        return Response.ok(sb.toString(), MediaType.APPLICATION_JSON).build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static StringBuilder startFeatureCollection(long total, boolean truncated) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"type\":\"FeatureCollection\",")
          .append("\"total\":").append(total).append(',')
          .append("\"truncated\":").append(truncated).append(',')
          .append("\"features\":[");
        return sb;
    }

    /**
     * Builds a GeoJSON Feature for one {@code "lat;lon"} location value, or returns
     * {@code null} when the coordinates cannot be parsed.
     */
    private static String buildFeature(String featureId, String sourceId, int docId,
                                       String name, String locValue,
                                       Map<String, List<String>> meta) {
        String[] parts = locValue.split(";");
        if (parts.length < 2) return null;
        double lat, lon;
        try {
            lat = Double.parseDouble(parts[0].trim());
            lon = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }

        String altStr = firstValue(meta, ALTITUDE_KEY);
        String timestamp = firstValue(meta, "dcterms:created");
        String nameSafe  = jsonString(name != null ? name : "");
        String tsSafe    = timestamp != null ? jsonString(timestamp) : "null";

        StringBuilder f = new StringBuilder(192);
        f.append("{\"type\":\"Feature\",")
         .append("\"id\":").append(jsonString(featureId)).append(',')
         .append("\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
         .append(lon).append(',').append(lat);
        if (altStr != null) {
            try { f.append(',').append(Double.parseDouble(altStr.trim())); }
            catch (NumberFormatException ignored) {}
        }
        f.append("]},")
         .append("\"properties\":{")
         .append("\"sourceId\":").append(jsonString(sourceId)).append(',')
         .append("\"docId\":").append(docId).append(',')
         .append("\"name\":").append(nameSafe).append(',')
         .append("\"timestamp\":").append(tsSafe)
         .append("}}");
        return f.toString();
    }

    private static String firstValue(Map<String, List<String>> meta, String key) {
        List<String> vals = meta.get(key);
        return (vals != null && !vals.isEmpty()) ? vals.get(0) : null;
    }

    private static String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    private static Response notFound(String msg) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":" + jsonString(msg) + "}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
