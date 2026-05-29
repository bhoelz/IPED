package iped.engine.webapi;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.core.CaseContext;
import iped.engine.core.ProcessingOrchestrator;
import iped.engine.core.ResourceManager;
import iped.engine.webapi.json.CaseStatsJSON;
import iped.engine.webapi.json.GlobalStatsJSON;

@Api(value = "Statistics")
@Path("stats")
public class Stats {

    static ProcessingOrchestrator orchestrator() {
        return ProcessingOrchestrator.getInstance();
    }

    @ApiOperation(value = "Get global statistics")
    @GET
    @Path("global")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response getGlobalStats() {
        GlobalStatsJSON stats = new GlobalStatsJSON();
        ProcessingOrchestrator orchestrator = orchestrator();
        ResourceManager resourceMgr = orchestrator.getResourceManager();

        stats.setActiveCases(orchestrator.getActiveCaseCount());
        stats.setMaxConcurrentCases(resourceMgr.getMaxConcurrentCases());
        stats.setTotalMemoryUsed(resourceMgr.getTotalMemoryUsage());
        stats.setMaxMemoryPerCase(resourceMgr.getMaxMemoryPerCase());

        return Response.ok(stats).build();
    }

    @ApiOperation(value = "Get case statistics")
    @GET
    @Path("case/{caseId}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response getCaseStats(@PathParam("caseId") String caseIdStr) {
        try {
            UUID caseId = UUID.fromString(caseIdStr);
            CaseContext context = orchestrator().getCaseContext(caseId);
            if (context == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            CaseStatsJSON stats = new CaseStatsJSON();
            stats.setCaseId(caseId.toString());
            stats.setState(context.getState().toString());

            ResourceManager resourceMgr = orchestrator().getResourceManager();
            stats.setMemoryUsage(resourceMgr.getMemoryUsage(caseId));

            return Response.ok(stats).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}
