package iped.engine.webapi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;

@Path("")
public class Root {
    @GET
    public static Response root() throws URISyntaxException {
        return Response.temporaryRedirect(new URI("./swagger.json")).build();
    }
}
