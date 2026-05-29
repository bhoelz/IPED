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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS resource for schema operations.
 * Exposes REST endpoints for configuration and CLI schema management.
 */
@Path("/api/v1/schemas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SchemaResource {
    private final SchemaAPIController controller;

    public SchemaResource() {
        this.controller = new SchemaAPIController();
    }

    @GET
    public Response listSchemas() {
        List<Map<String, Object>> schemas = controller.listSchemas();
        Map<String, Object> response = createListResponse(schemas);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{componentName}")
    public Response getSchema(@PathParam("componentName") String componentName) {
        Map<String, Object> result = controller.getSchema(componentName);
        return Response.ok(result).build();
    }

    @GET
    @Path("/{componentName}/ui")
    public Response getUISchema(@PathParam("componentName") String componentName) {
        Map<String, Object> result = controller.getSchema(componentName);
        if ((boolean) result.get("success")) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("uiSchema", result.get("uiSchema"));
            return Response.ok(response).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity(result).build();
    }

    @POST
    @Path("/{componentName}/validate")
    public Response validateConfiguration(
            @PathParam("componentName") String componentName,
            Map<String, Object> request) {
        String configJson = ((Map<String, String>) request.get("config")).toString();
        Map<String, Object> result = controller.validateConfiguration(componentName, configJson);
        return Response.ok(result).build();
    }

    @GET
    @Path("/category/{category}")
    public Response getSchemasByCategory(@PathParam("category") String category) {
        List<Map<String, Object>> schemas = controller.getSchemasByCategory(category);
        Map<String, Object> response = createListResponse(schemas);
        response.put("category", category);
        return Response.ok(response).build();
    }

    @GET
    @Path("/../cli-schemas")
    public Response listCLISchemas() {
        List<Map<String, Object>> schemas = controller.listCLISchemas();
        Map<String, Object> response = createListResponse(schemas);
        return Response.ok(response).build();
    }

    private Map<String, Object> createListResponse(List<Map<String, Object>> items) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("schemas", items);
        response.put("count", items.size());
        return response;
    }
}
