package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.engine.webapi.json.DataListJSON;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Api(value = "Categories")
@Path("categories")
public class Categories {

    @ApiOperation(value = "List categories")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataListJSON<String> get() throws Exception {
        Set<String> categoriesSet = new TreeSet<>();
        for (var source : Sources.services().sources().listSources()) {
            categoriesSet.addAll(Sources.getSource(source.getId()).getLeafCategories());
        }
        List<String> categories = categoriesSet.stream().toList();
        DataListJSON<String> result = new DataListJSON<String>(categories);

        return result;
    }
}
