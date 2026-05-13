package iped.engine.webapi;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.webapi.json.DataListJSON;

@Api(value = "Categories")
@Path("categories")
public class Categories {

    @ApiOperation(value = "List categories")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataListJSON<String> get() throws Exception {

        List<String> categories = Sources.multiSource.getLeafCategories();
        DataListJSON<String> result = new DataListJSON<String>(categories);

        return result;
    }
}
