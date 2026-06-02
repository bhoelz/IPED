package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iped.engine.webapi.json.DocIDJSON;
import iped.engine.webapi.json.SourceToIDsJSON;
import iped.engine.webapi.spi.DocRef;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

@Api(value = "Selection")
@Path("selection")
public class Selection {

    @ApiOperation(value = "List selected documents")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SourceToIDsJSON get() throws Exception {
        List<DocIDJSON> docs = new ArrayList<DocIDJSON>();
        for (DocRef doc : Sources.services().selection().getSelected()) {
            docs.add(new DocIDJSON(doc.getSource(), doc.getId()));
        }
        return new SourceToIDsJSON(docs);
    }

    @ApiOperation(value = "Add documents to selection")
    @PUT
    @Path("add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@ApiParam(required = true) DocIDJSON[] docs) {
        List<DocRef> refs = new ArrayList<>();
        for (DocIDJSON d : docs) {
            refs.add(new DocRef(d.getSource(), d.getId()));
        }
        Sources.services().selection().add(refs);
        return Response.ok().build();
    }

    @ApiOperation(value = "Remove documents from selection")
    @PUT
    @Path("remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@ApiParam(required = true) DocIDJSON[] docs) {
        List<DocRef> refs = new ArrayList<>();
        for (DocIDJSON d : docs) {
            refs.add(new DocRef(d.getSource(), d.getId()));
        }
        Sources.services().selection().remove(refs);
        return Response.ok().build();
    }

}

