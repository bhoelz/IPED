package iped.engine.webapi;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.webapi.json.DocIDJSON;
import iped.engine.webapi.json.SourceToIDsJSON;
import iped.engine.webapi.spi.DocRef;

@Api(value = "Search")
@Path("search")
public class Search {

    @DefaultValue("")
    @QueryParam("q")
    String q;
    @DefaultValue("")
    @QueryParam("sourceID")
    String sourceID;

    @ApiOperation(value = "Search documents")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SourceToIDsJSON doSearch() throws Exception {
        List<DocIDJSON> docs = new ArrayList<DocIDJSON>();
        for (DocRef doc : Sources.services().search().search(q, sourceID)) {
            docs.add(new DocIDJSON(doc.getSource(), doc.getId()));
        }
        return new SourceToIDsJSON(docs);
    }
}

