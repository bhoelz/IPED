package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.data.IItem;
import iped.engine.webapi.json.v2.*;
import iped.engine.webapi.spi.RenditionDescriptor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Api(value = "ViewerSessions")
@Path("sources/{sourceID}/docs/{id}")
public class ViewerSessions {

    @ApiOperation(value = "Open a viewer session for an item")
    @POST
    @Path("viewer/sessions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response open(
            @PathParam("sourceID") String sourceID,
            @PathParam("id") int id,
            ViewerOpenRequestJSON request,
            @Context UriInfo uriInfo) {

        IIPEDSource source = Sources.getSource(sourceID);
        IItem item = source.getItemByID(id);
        String mimeType = item.getMediaType() != null ? item.getMediaType().toString() : "application/octet-stream";

        String baseUrl = resolveBaseUrl(uriInfo);

        List<String> highlightTerms = request != null && request.getHighlightTerms() != null
                ? request.getHighlightTerms() : List.of();
        String preferredViewerId = request != null ? request.getPreferredViewerId() : null;

        Map<String, Object> sessionData = Sources.services().viewerSessions()
                .openSession(sourceID, id, mimeType, highlightTerms, preferredViewerId, baseUrl);

        ViewerOpenResponseJSON resp = toResponse(sessionData);
        return Response.ok(resp).build();
    }

    @ApiOperation(value = "Search inside a viewer session")
    @POST
    @Path("viewer/sessions/{sessionId}/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(
            @PathParam("sourceID") String sourceID,
            @PathParam("id") int id,
            @PathParam("sessionId") String sessionId,
            ViewerSearchRequestJSON request) {

        Map<String, Object> result = Sources.services().viewerSessions()
                .search(sessionId,
                        request != null ? request.getTerm() : "",
                        request != null && request.isCaseSensitive());

        ViewerSearchResponseJSON resp = new ViewerSearchResponseJSON();
        resp.setTotalHits(intVal(result, "totalHits"));
        resp.setCurrentHit(intVal(result, "currentHit"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranges = (List<Map<String, Object>>) result.getOrDefault("hitRanges", List.of());
        List<HitRangeJSON> hitRanges = new ArrayList<>();
        for (Map<String, Object> r : ranges) {
            HitRangeJSON hr = new HitRangeJSON();
            hr.setStart(intVal(r, "start"));
            hr.setEnd(intVal(r, "end"));
            hr.setPage(intVal(r, "page"));
            hitRanges.add(hr);
        }
        resp.setHitRanges(hitRanges);
        return Response.ok(resp).build();
    }

    @ApiOperation(value = "Navigate to next or previous hit in a viewer session")
    @POST
    @Path("viewer/sessions/{sessionId}/hits:navigate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response navigateHit(
            @PathParam("sourceID") String sourceID,
            @PathParam("id") int id,
            @PathParam("sessionId") String sessionId,
            ViewerNavigateHitRequestJSON request) {

        Map<String, Object> result = Sources.services().viewerSessions()
                .navigateHit(sessionId,
                        request != null ? request.getDirection() : "next",
                        request != null && request.isWrap());

        ViewerHitStateJSON resp = new ViewerHitStateJSON();
        resp.setTotalHits(intVal(result, "totalHits"));
        resp.setCurrentHit(intVal(result, "currentHit"));
        return Response.ok(resp).build();
    }

    private ViewerOpenResponseJSON toResponse(Map<String, Object> data) {
        ViewerOpenResponseJSON resp = new ViewerOpenResponseJSON();
        resp.setViewerSessionId((String) data.get("viewerSessionId"));
        resp.setViewerId((String) data.get("viewerId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> caps = (Map<String, Object>) data.get("capabilities");
        if (caps != null) {
            ViewerCapabilitiesJSON capJson = new ViewerCapabilitiesJSON();
            capJson.setSearch(Boolean.TRUE.equals(caps.get("search")));
            capJson.setHitsMode((String) caps.get("hitsMode"));
            ViewerToolbarStateJSON toolbar = new ViewerToolbarStateJSON();
            toolbar.setSupported(Boolean.TRUE.equals(caps.get("toolbarSupported")));
            toolbar.setVisible(Boolean.TRUE.equals(caps.get("toolbarVisibleByDefault")));
            capJson.setToolbar(toolbar);
            resp.setCapabilities(capJson);
        }

        @SuppressWarnings("unchecked")
        List<RenditionDescriptor> descs = (List<RenditionDescriptor>) data.get("renditions");
        if (descs != null) {
            List<RenditionLinkJSON> links = new ArrayList<>();
            for (RenditionDescriptor d : descs) {
                RenditionLinkJSON link = new RenditionLinkJSON();
                link.setKind(d.getKind());
                link.setUrl(d.getUrl());
                links.add(link);
            }
            resp.setRenditions(links);
        }
        return resp;
    }

    private String resolveBaseUrl(UriInfo uriInfo) {
        URI base = uriInfo.getBaseUri();
        return base.getScheme() + "://" + base.getHost()
                + (base.getPort() > 0 ? ":" + base.getPort() : "");
    }

    private static int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }
}
