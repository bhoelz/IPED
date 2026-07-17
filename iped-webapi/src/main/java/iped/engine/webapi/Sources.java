package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iped.data.IIPEDSource;
import iped.engine.webapi.json.DataListJSON;
import iped.engine.webapi.json.SourceJSON;
import iped.engine.webapi.spi.SourceDescriptor;
import iped.engine.webapi.spi.WebApiServices;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Api(value = "Sources")
@Path("sources")
public class Sources {

  public static void init(String urlToAskSources) throws Exception {
    services().sources().init(urlToAskSources);
  }

  static WebApiServices services() {
    return WebApiServicesLocator.get();
  }

  public static IIPEDSource getSource(String sourceID) {
    return services().sources().getSourceHandle(sourceID);
  }

  @ApiOperation(value = "List sources")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public static DataListJSON<SourceJSON> listSources() throws Exception {
    List<SourceJSON> data = new ArrayList<>();
    for (SourceDescriptor source : services().sources().listSources()) {
      SourceJSON sourceJSON = new SourceJSON();
      sourceJSON.setId(source.getId());
      sourceJSON.setPath(source.getPath());
      data.add(sourceJSON);
    }
    return new DataListJSON<SourceJSON>(data);
  }

  @ApiOperation(value = "Add source")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public static synchronized Response addSource(@ApiParam(required = true) SourceJSON sourcejson) {
    services().sources().addSource(new SourceDescriptor(sourcejson.getId(), sourcejson.getPath()));
    return Response.ok().build();
  }

  @ApiOperation(value = "Get source's properties")
  @GET
  @Path("{sourceID}")
  @Produces(MediaType.APPLICATION_JSON)
  public static SourceJSON getone(@PathParam("sourceID") String sourceID) throws Exception {
    SourceDescriptor source = services().sources().getSource(sourceID);
    SourceJSON result = new SourceJSON();
    result.setId(source.getId());
    result.setPath(source.getPath());
    return result;
  }
}
