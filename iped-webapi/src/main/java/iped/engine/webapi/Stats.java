package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.webapi.json.CaseStatsJSON;
import iped.engine.webapi.json.GlobalStatsJSON;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Api(value = "Statistics")
@Path("stats")
public class Stats {

  static IProcessingService service() {
    return ProcessingServiceRegistry.get();
  }

  @ApiOperation(value = "Get global statistics")
  @GET
  @Path("global")
  @Produces(MediaType.APPLICATION_JSON)
  public static Response getGlobalStats() {
    IProcessingService svc = service();
    GlobalStatsJSON stats = new GlobalStatsJSON();
    stats.setActiveCases(svc.getActiveCaseCount());
    stats.setMaxConcurrentCases(svc.getMaxConcurrentCases());
    stats.setTotalMemoryUsed(svc.getTotalMemoryUsage());
    stats.setMaxMemoryPerCase(svc.getMaxMemoryPerCase());
    return Response.ok(stats).build();
  }

  @ApiOperation(value = "Get case statistics")
  @GET
  @Path("case/{caseId}")
  @Produces(MediaType.APPLICATION_JSON)
  public static Response getCaseStats(@PathParam("caseId") String caseIdStr) {
    try {
      UUID caseId = UUID.fromString(caseIdStr);
      IProcessingService svc = service();
      if (!svc.caseExists(caseId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      CaseStatsJSON stats = new CaseStatsJSON();
      stats.setCaseId(caseId.toString());
      stats.setState(svc.getCaseState(caseId));
      stats.setMemoryUsage(svc.getMemoryUsage(caseId));

      return Response.ok(stats).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }
}
