package iped.engine.webapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iped.data.IIPEDSource;
import iped.data.IItem;
import iped.engine.data.IPEDSource;
import iped.engine.webapi.json.v2.ItemMetadataJSON;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * v2 item metadata endpoint.
 *
 * <p>Unlike {@code GET /sources/{sourceID}/docs/{id}} (which returns raw Lucene fields), this
 * endpoint returns a structured JSON object with typed fields from the {@code IItem} interface,
 * plus the full metadata map.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * GET /v2/sources/case-1/items/42
 * </pre>
 */
@Api(value = "Items v2")
@Path("v2/sources/{sourceId}/items")
public class ItemsV2 {

  @ApiOperation(
      value = "Get full item metadata",
      notes =
          "Returns all structured metadata for the item including dates, hashes, "
              + "categories, bookmarks, and the full Tika metadata map.")
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getItem(@PathParam("sourceId") String sourceId, @PathParam("id") int id)
      throws Exception {
    IIPEDSource sourceHandle = Sources.services().sources().getSourceHandle(sourceId);
    if (sourceHandle == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("{\"error\":\"Source not found: " + sourceId + "\"}")
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    IItem item = ((IPEDSource) sourceHandle).getItemByID(id);
    if (item == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("{\"error\":\"Item " + id + " not found in source " + sourceId + "\"}")
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    ItemMetadataJSON json = toJSON(sourceId, id, item, sourceHandle);
    return Response.ok(json).build();
  }

  private static ItemMetadataJSON toJSON(String sourceId, int id, IItem item, IIPEDSource source) {
    ItemMetadataJSON j = new ItemMetadataJSON();
    j.setSourceId(sourceId);
    j.setId(id);
    j.setName(item.getName());
    j.setPath(item.getPath());
    j.setMediaType(item.getMediaTypeString());
    j.setSize(item.getLength());
    j.setHash(item.getHash());
    j.setModDate(item.getModDate());
    j.setCreationDate(item.getCreationDate());
    j.setAccessDate(item.getAccessDate());
    j.setChangeDate(item.getChangeDate());
    j.setDeleted(item.isDeleted());
    j.setCarved(item.isCarved());
    j.setDir(item.isDir());
    j.setSubItem(item.isSubItem());
    j.setParentId(item.getParentId());
    j.setParentIds(item.getParentIds());
    j.setCategories(item.getCategorySet());
    j.setHasChildren(item.hasChildren());
    j.setHasPreview(item.hasPreview());
    j.setMetadata(item.getMetadataMap());

    // Bookmark and selection state from the source's bookmark index
    j.setBookmarks(source.getBookmarks().getBookmarkList(id));
    j.setSelected(source.getBookmarks().isChecked(id));

    return j;
  }

  @ApiOperation(
      value = "List categories for a source",
      notes = "Returns all distinct category names recorded across all items in the source.")
  @GET
  @Path("categories")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCategories(@PathParam("sourceId") String sourceId) throws Exception {
    IIPEDSource sourceHandle = Sources.services().sources().getSourceHandle(sourceId);
    if (sourceHandle == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("{\"error\":\"Source not found: " + sourceId + "\"}")
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    List<String> categories = new ArrayList<>(sourceHandle.getLeafCategories());
    categories.sort(String::compareToIgnoreCase);
    return Response.ok(categories).build();
  }
}
