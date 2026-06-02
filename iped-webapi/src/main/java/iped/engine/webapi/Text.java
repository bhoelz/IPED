package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.OutputStream;

@Api(value = "Documents")
@Path("/sources/{sourceID}/docs/{id}/text")
public class Text {

    @ApiOperation(value = "Get document's content converted as text")
    @GET
    @Produces(MediaType.TEXT_PLAIN + "; charset=UTF-8")
    public static StreamingOutput content(@PathParam("sourceID") String sourceID, @PathParam("id") int id)
            throws Exception {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try {
                    Sources.services().text().writeText(sourceID, id, output);
                } catch (Exception e) {
                    throw new WebApplicationException(e);
                }
            }
        };
    }
}
