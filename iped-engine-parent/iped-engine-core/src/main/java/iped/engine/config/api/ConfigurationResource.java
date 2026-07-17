/*
 * Copyright 2012-2014, Luis Filipe da Cruz Nassif
 *
 * This file is part of Indexador e Processador de Evidências Digitais (IPED).
 *
 * IPED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IPED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IPED.  If not, see <http://www.gnu.org/licenses/>.
 */
package iped.engine.config.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.schema.ConfigurationDiffMerge;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * JAX-RS resource for configuration management. Exposes REST endpoints for configuration
 * operations, backups, and merging.
 */
@Path("/api/v1/configurations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigurationResource {
  private final ConfigurationAPIController controller;
  private final ConfigurationDiffMerge diffMerge;
  private final ObjectMapper mapper = new ObjectMapper();

  public ConfigurationResource() {
    ConfigurationManager configManager = ConfigurationManager.get();
    this.controller = new ConfigurationAPIController(configManager);
    this.diffMerge = new ConfigurationDiffMerge();
  }

  @GET
  public Response getCurrentConfigurations() {
    Map<String, Object> result = controller.getCurrentConfiguration();
    return Response.ok(result).build();
  }

  @GET
  @Path("/{componentName}")
  public Response getConfiguration(@PathParam("componentName") String componentName) {
    Map<String, Object> result = controller.getConfiguration(componentName);
    if ((boolean) result.get("success")) {
      return Response.ok(result).build();
    }
    return Response.status(Response.Status.NOT_FOUND).entity(result).build();
  }

  @GET
  @Path("/{componentName}/export")
  public Response exportConfiguration(@PathParam("componentName") String componentName) {
    Map<String, Object> result = controller.exportConfiguration(componentName);
    if ((boolean) result.get("success")) {
      return Response.ok(result).build();
    }
    return Response.status(Response.Status.NOT_FOUND).entity(result).build();
  }

  @GET
  @Path("/export/all")
  public Response exportAllConfigurations() {
    Map<String, Object> result = controller.exportAllConfigurations();
    return Response.ok(result).build();
  }

  @GET
  @Path("/metadata")
  public Response getConfigurationMetadata() {
    Map<String, Object> result = controller.getConfigurationMetadata();
    return Response.ok(result).build();
  }

  @POST
  @Path("/backup")
  public Response createBackup(Map<String, Object> request) {
    String backupPath = (String) request.get("backupPath");
    if (backupPath == null) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("error", "backupPath required");
      return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    Map<String, Object> result = controller.createBackup(backupPath);
    if ((boolean) result.get("success")) {
      return Response.ok(result).build();
    }
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
  }

  @POST
  @Path("/diff")
  public Response compareConfigurations(Map<String, Object> request) {
    try {
      ObjectNode config1 = mapper.valueToTree(request.get("config1"));
      ObjectNode config2 = mapper.valueToTree(request.get("config2"));

      ConfigurationDiffMerge.ConfigurationDiff diff = diffMerge.diff(config1, config2);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("added", diff.getAdded());
      response.put("removed", diff.getRemoved());
      response.put("modified", diff.getModified());
      response.put("totalChanges", diff.getTotalChanges());

      return Response.ok(response).build();
    } catch (Exception e) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("error", "Invalid configuration format: " + e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
  }

  @POST
  @Path("/merge")
  public Response mergeConfigurations(Map<String, Object> request) {
    try {
      ObjectNode baseConfig = mapper.valueToTree(request.get("baseConfig"));
      ObjectNode config1 = mapper.valueToTree(request.get("config1"));
      ObjectNode config2 = mapper.valueToTree(request.get("config2"));

      ConfigurationDiffMerge.MergeResult result = diffMerge.merge(baseConfig, config1, config2);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("mergedConfig", result.getMergedConfig());
      response.put("applied", result.getApplied());
      response.put("conflicts", result.getConflicts());
      response.put("hasConflicts", result.hasConflicts());
      response.put("conflictCount", result.getConflictCount());

      return Response.ok(response).build();
    } catch (Exception e) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("error", "Invalid configuration format: " + e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
  }
}
