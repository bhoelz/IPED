package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iped.engine.webapi.json.v2.SearchResultItemJSON;
import iped.engine.webapi.json.v2.SearchResultPageJSON;
import iped.engine.webapi.spi.SearchPage;
import iped.engine.webapi.spi.SearchResultItem;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * v2 paginated search endpoint.
 *
 * <p>Unlike the legacy {@code GET /search} endpoint (which returns all matching
 * document IDs with no metadata), this endpoint:
 * <ul>
 *   <li>Supports {@code offset}/{@code limit} pagination (max 1000 per page).</li>
 *   <li>Returns rich item metadata per result so the UI can render the list
 *       without additional round-trips.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>
 * GET /v2/search?q=murder&offset=0&limit=50
 * GET /v2/search?q=murder&sourceId=case-1&offset=50&limit=50
 * </pre>
 */
@Api(value = "Search v2")
@Path("v2/search")
public class SearchV2 {

    private static final int MAX_LIMIT = 1000;

    @DefaultValue("")
    @QueryParam("q")
    String q;

    @DefaultValue("")
    @QueryParam("sourceId")
    String sourceId;

    @DefaultValue("0")
    @QueryParam("offset")
    int offset;

    @DefaultValue("50")
    @QueryParam("limit")
    int limit;

    @ApiOperation(
            value = "Paginated search with metadata",
            notes = "Searches all open sources (or a single source when sourceId is given) and " +
                    "returns a page of results with per-item metadata. " +
                    "offset must be ≥ 0; limit must be between 1 and 1000."
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(
            @ApiParam(value = "Lucene query string", example = "murder") @QueryParam("q") String qParam,
            @ApiParam(value = "Restrict to a specific source ID; omit for all sources") @QueryParam("sourceId") String sourceIdParam,
            @ApiParam(value = "Zero-based start index", example = "0") @QueryParam("offset") @DefaultValue("0") int offsetParam,
            @ApiParam(value = "Items per page (1–1000)", example = "50") @QueryParam("limit") @DefaultValue("50") int limitParam
    ) throws Exception {
        // Coerce params (JAX-RS injects into fields AND method params; use method params as primary)
        String query    = qParam       != null ? qParam       : "";
        String srcId    = sourceIdParam != null ? sourceIdParam : "";
        int    off      = Math.max(0, offsetParam);
        int    lim      = Math.min(Math.max(1, limitParam), MAX_LIMIT);

        if (Sources.services().sources().listSources().isEmpty()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\":\"No cases are currently open. POST a source to /sources first.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        SearchPage page = Sources.services().search().searchPaginated(query, srcId, off, lim);
        return Response.ok(toJSON(page)).build();
    }

    private static SearchResultPageJSON toJSON(SearchPage page) {
        List<SearchResultItemJSON> itemsJSON = new ArrayList<>(page.getItems().size());
        for (SearchResultItem item : page.getItems()) {
            SearchResultItemJSON j = new SearchResultItemJSON();
            j.setSourceId(item.getSourceId());
            j.setId(item.getId());
            j.setName(item.getName());
            j.setPath(item.getPath());
            j.setMediaType(item.getMediaType());
            j.setSize(item.getSize());
            j.setHash(item.getHash());
            j.setModDate(item.getModDate());
            j.setCreationDate(item.getCreationDate());
            j.setDeleted(item.isDeleted());
            j.setDir(item.isDir());
            j.setCategories(item.getCategories());
            itemsJSON.add(j);
        }
        SearchResultPageJSON result = new SearchResultPageJSON();
        result.setTotal(page.getTotal());
        result.setOffset(page.getOffset());
        result.setLimit(page.getLimit());
        result.setItems(itemsJSON);
        return result;
    }
}
