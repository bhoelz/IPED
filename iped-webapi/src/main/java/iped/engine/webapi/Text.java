package iped.engine.webapi;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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
