package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.engine.webapi.json.DocPropsJSON;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Api(value = "Documents")
@Path("sources/{sourceID}/docs")
public class Docs {

    @ApiOperation(value = "Get document's properties")
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static DocPropsJSON properties(@PathParam("sourceID") String sourceID, @PathParam("id") int id)
            throws IOException {
        IIPEDSource source = Sources.getSource(sourceID);
        int luceneID = source.getLuceneId(id);
        IndexReader reader = (IndexReader) source.getIndexReaderHandle();
        Document doc = reader.storedFields().document(luceneID);

        DocPropsJSON result = new DocPropsJSON();
        result.setSource(sourceID);
        result.setId(id);
        result.setLuceneId(luceneID);
        Map<String, String[]> properties = new HashMap<String, String[]>();
        for (IndexableField field : doc.getFields()) {
            String[] values = doc.getValues(field.name());
            properties.put(field.name(), values);
        }
        result.setProperties(properties);

        result.setBookmarks(source.getBookmarks().getBookmarkList(id));
        result.setSelected(source.getBookmarks().isChecked(id));

        return result;
    }
}

