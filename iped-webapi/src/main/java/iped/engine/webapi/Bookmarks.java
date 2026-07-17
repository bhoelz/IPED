package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iped.engine.webapi.json.DataListJSON;
import iped.engine.webapi.json.DocIDJSON;
import iped.engine.webapi.json.SourceToIDsJSON;
import iped.engine.webapi.spi.DocRef;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Api(value = "Bookmarks")
@Path("bookmarks")
public class Bookmarks {

  @ApiOperation(value = "List bookmarks")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public DataListJSON<String> getAll() {
    Set<String> bookmarks = Sources.services().bookmarks().listBookmarks();
    String[] IDs = bookmarks.toArray(new String[0]);
    return new DataListJSON<String>(IDs);
  }

  @ApiOperation(value = "List bookmark documents")
  @GET
  @Path("{bookmark}")
  @Produces(MediaType.APPLICATION_JSON)
  public SourceToIDsJSON get(@PathParam("bookmark") String bookmark) throws Exception {
    List<DocIDJSON> docs = new ArrayList<DocIDJSON>();
    for (DocRef doc : Sources.services().bookmarks().listBookmarkDocs(bookmark)) {
      docs.add(new DocIDJSON(doc.getSource(), doc.getId()));
    }

    return new SourceToIDsJSON(docs);
  }

  @ApiOperation(value = "Add documents to bookmark")
  @PUT
  @Path("{bookmark}/add")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response insertLabel(
      @PathParam("bookmark") String bookmark, @ApiParam(required = true) DocIDJSON[] docs) {
    List<DocRef> refs = new ArrayList<>();
    for (DocIDJSON d : docs) {
      refs.add(new DocRef(d.getSource(), d.getId()));
    }
    Sources.services().bookmarks().add(bookmark, refs);
    return Response.ok().build();
  }

  @ApiOperation(value = "Remove documents from bookmark")
  @PUT
  @Path("{bookmark}/remove")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeLabel(
      @PathParam("bookmark") String bookmark, @ApiParam(required = true) DocIDJSON[] docs) {
    List<DocRef> refs = new ArrayList<>();
    for (DocIDJSON d : docs) {
      refs.add(new DocRef(d.getSource(), d.getId()));
    }
    Sources.services().bookmarks().remove(bookmark, refs);
    return Response.ok().build();
  }

  @ApiOperation(value = "Create bookmark")
  @POST
  @Path("{bookmark}")
  public Response addLabel(@PathParam("bookmark") String bookmark) {
    Sources.services().bookmarks().create(bookmark);
    return Response.ok().build();
  }

  @ApiOperation(value = "Delete bookmark")
  @DELETE
  @Path("{bookmark}")
  public Response delLabel(@PathParam("bookmark") String bookmark) {
    Sources.services().bookmarks().delete(bookmark);
    return Response.ok().build();
  }

  @ApiOperation(value = "Rename bookmark")
  @PUT
  @Path("{old}/rename/{new}")
  public Response changeLabel(
      @PathParam("old") String oldLabel, @PathParam("new") String newLabel) {
    Sources.services().bookmarks().rename(oldLabel, newLabel);
    return Response.ok().build();
  }
}
