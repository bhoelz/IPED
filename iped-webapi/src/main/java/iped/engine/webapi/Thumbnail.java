package iped.engine.webapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.data.IItem;

@Api(value = "Documents")
@Path("sources/{sourceID}/docs/{id}/thumb")
public class Thumbnail {

    @ApiOperation(value = "Get document's thumbnail")
    @GET
    @Produces("image/jpg")
    public StreamingOutput content(@PathParam("sourceID") String sourceID, @PathParam("id") int id)
            throws IOException, URISyntaxException {

        IIPEDSource source = Sources.getSource(sourceID);
        IItem item = source.getItemByID(id);
        final byte[] thumb = item.getThumb() != null ? item.getThumb() : new byte[0];
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException, WebApplicationException {
                IOUtils.copy(new ByteArrayInputStream(thumb), arg0);
            }
        };
    }
}
