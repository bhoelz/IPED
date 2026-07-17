package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iped.engine.webapi.json.CaseStatusJSON;
import iped.engine.webapi.json.DataListJSON;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Api(value = "Cases")
@Path("cases")
public class Cases {

  static IProcessingService service() {
    return ProcessingServiceRegistry.get();
  }

  @ApiOperation(value = "List all active cases")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public static DataListJSON<String> listCases() {
    List<String> caseIds = new ArrayList<>();
    for (UUID uuid : service().getActiveCaseIds()) {
      caseIds.add(uuid.toString());
    }
    return new DataListJSON<>(caseIds);
  }

  @ApiOperation(value = "Get case status")
  @GET
  @Path("{caseId}")
  @Produces(MediaType.APPLICATION_JSON)
  public static Response getCaseStatus(@PathParam("caseId") String caseIdStr) {
    try {
      UUID caseId = UUID.fromString(caseIdStr);
      if (!service().caseExists(caseId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      CaseStatusJSON status = new CaseStatusJSON();
      status.setCaseId(caseId.toString());
      status.setState(service().getCaseState(caseId));
      return Response.ok(status).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @ApiOperation(value = "Pause case processing")
  @POST
  @Path("{caseId}/pause")
  @Produces(MediaType.APPLICATION_JSON)
  public static Response pauseCase(@PathParam("caseId") String caseIdStr) {
    try {
      UUID caseId = UUID.fromString(caseIdStr);
      if (!service().caseExists(caseId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      service().pauseCase(caseId);
      return Response.ok().build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @ApiOperation(value = "Resume case processing")
  @POST
  @Path("{caseId}/resume")
  @Produces(MediaType.APPLICATION_JSON)
  public static Response resumeCase(@PathParam("caseId") String caseIdStr) {
    try {
      UUID caseId = UUID.fromString(caseIdStr);
      if (!service().caseExists(caseId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      service().resumeCase(caseId);
      return Response.ok().build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @ApiOperation(value = "Update case memory quota")
  @PUT
  @Path("{caseId}/quota")
  @Produces(MediaType.APPLICATION_JSON)
  public static Response updateQuota(
      @PathParam("caseId") String caseIdStr, @ApiParam(required = true) long maxMemoryBytes) {
    try {
      UUID caseId = UUID.fromString(caseIdStr);
      if (!service().caseExists(caseId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      return Response.ok().build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }
}
