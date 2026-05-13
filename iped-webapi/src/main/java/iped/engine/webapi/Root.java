package iped.engine.webapi;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("")
public class Root {
    @GET
    public static Response root() throws URISyntaxException {
        return Response.temporaryRedirect(new URI("./swagger.json")).build();
    }
}
