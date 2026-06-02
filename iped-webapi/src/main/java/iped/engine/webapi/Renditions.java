package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.OutputStream;

@Api(value = "Renditions")
@Path("sources/{sourceID}/docs/{id}/renditions")
public class Renditions {

    @ApiOperation(value = "Get text rendition of a document")
    @GET
    @Path("text")
    @Produces(MediaType.TEXT_PLAIN + "; charset=UTF-8")
    public Response text(
            @PathParam("sourceID") String sourceID,
            @PathParam("id") int id) {
        return streamRendition(sourceID, id, "text", -1, 0, MediaType.TEXT_PLAIN + "; charset=UTF-8");
    }

    @ApiOperation(value = "Get HTML rendition of a document")
    @GET
    @Path("html")
    @Produces(MediaType.TEXT_HTML + "; charset=UTF-8")
    public Response html(
            @PathParam("sourceID") String sourceID,
            @PathParam("id") int id) {
        return streamRendition(sourceID, id, "html", -1, 0, MediaType.TEXT_HTML + "; charset=UTF-8");
    }

    @ApiOperation(value = "Get image rendition of a document (PNG). Use ?page= for multi-page formats.")
    @GET
    @Path("image")
    @Produces("image/png")
    public Response image(
            @PathParam("sourceID") String sourceID,
            @PathParam("id") int id,
            @ApiParam("0-based page index (default 0)")
            @QueryParam("page") @DefaultValue("0") int page,
            @ApiParam("Pixel width hint for rendering (0 = renderer default)")
            @QueryParam("width") @DefaultValue("0") int width) {
        return streamRendition(sourceID, id, "image", page, width, "image/png");
    }

    @ApiOperation(value = "Get PDF rendition of a document")
    @GET
    @Path("pdf")
    @Produces("application/pdf")
    public Response pdf(
            @PathParam("sourceID") String sourceID,
            @PathParam("id") int id) {
        return streamRendition(sourceID, id, "pdf", -1, 0, "application/pdf");
    }

    private Response streamRendition(String sourceID, int id, String kind, int page, int width,
            String mediaType) {
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try {
                    Sources.services().renditions().writeRendition(sourceID, id, kind, page, width, output);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException(e.getMessage(),
                            Response.Status.NOT_FOUND);
                } catch (Exception e) {
                    throw new WebApplicationException(e,
                            Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        };
        return Response.ok(stream, mediaType).build();
    }
}
