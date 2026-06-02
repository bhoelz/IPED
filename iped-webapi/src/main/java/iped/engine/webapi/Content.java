package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.data.IItem;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

@Api(value = "Documents")
@Path("sources/{sourceID}/docs/{id}/content")
public class Content {

    @ApiOperation(value = "Get document's raw content")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response content(@PathParam("sourceID") String sourceID, @PathParam("id") int id)
            throws IOException, URISyntaxException {

        IIPEDSource source = Sources.getSource(sourceID);
        final IItem item = source.getItemByID(id);
        return Response.status(200).header("Content-Length", String.valueOf(item.getLength()))
                .header("Content-Disposition", "attachment; filename=\"" + item.getName() + "\"")
                .entity(new StreamingOutput() {
                    @Override
                    public void write(OutputStream arg0) throws IOException, WebApplicationException {
                        try (InputStream is = item.getBufferedInputStream()) {
                            IOUtils.copy(is, arg0);
                        }
                    }
                }).build();
    }
}

