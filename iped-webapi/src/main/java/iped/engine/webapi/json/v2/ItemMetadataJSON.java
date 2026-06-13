package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full metadata for a single item, returned by {@code GET /v2/sources/{sourceId}/items/{id}}.
 * Combines the structured fields from {@link iped.data.IItemReader} with the raw Lucene
 * metadata map for forensic attribute access.
 */
public class ItemMetadataJSON {

    private String sourceId;
    private int id;
    private String name;
    private String path;
    private String mediaType;
    private Long size;
    private String hash;
    private Date modDate;
    private Date creationDate;
    private Date accessDate;
    private Date changeDate;
    private boolean deleted;
    private boolean carved;
    private boolean dir;
    private boolean subItem;
    private Integer parentId;
    private List<Integer> parentIds;
    private Set<String> categories;
    private List<String> bookmarks;
    private boolean selected;
    private boolean hasChildren;
    private boolean hasPreview;
    private Map<String, List<String>> metadata;

    @ApiModelProperty("Source case identifier")
    public String getSourceId()               { return sourceId; }
    public void setSourceId(String v)         { this.sourceId = v; }

    @ApiModelProperty("Internal IPED item id")
    public int getId()                        { return id; }
    public void setId(int v)                  { this.id = v; }

    public String getName()                   { return name; }
    public void setName(String v)             { this.name = v; }

    public String getPath()                   { return path; }
    public void setPath(String v)             { this.path = v; }

    public String getMediaType()              { return mediaType; }
    public void setMediaType(String v)        { this.mediaType = v; }

    public Long getSize()                     { return size; }
    public void setSize(Long v)               { this.size = v; }

    @ApiModelProperty("Hash digest (MD5 by default)")
    public String getHash()                   { return hash; }
    public void setHash(String v)             { this.hash = v; }

    public Date getModDate()                  { return modDate; }
    public void setModDate(Date v)            { this.modDate = v; }

    public Date getCreationDate()             { return creationDate; }
    public void setCreationDate(Date v)       { this.creationDate = v; }

    public Date getAccessDate()               { return accessDate; }
    public void setAccessDate(Date v)         { this.accessDate = v; }

    @ApiModelProperty("Metadata change date (ctime on UNIX)")
    public Date getChangeDate()               { return changeDate; }
    public void setChangeDate(Date v)         { this.changeDate = v; }

    @ApiModelProperty("True if found in deleted/unallocated space")
    public boolean isDeleted()                { return deleted; }
    public void setDeleted(boolean v)         { this.deleted = v; }

    @ApiModelProperty("True if recovered via file carving")
    public boolean isCarved()                 { return carved; }
    public void setCarved(boolean v)          { this.carved = v; }

    @ApiModelProperty("True if this is a directory or container")
    public boolean isDir()                    { return dir; }
    public void setDir(boolean v)             { this.dir = v; }

    @ApiModelProperty("True if this item is a child of a container (zip entry, email attachment, …)")
    public boolean isSubItem()                { return subItem; }
    public void setSubItem(boolean v)         { this.subItem = v; }

    public Integer getParentId()              { return parentId; }
    public void setParentId(Integer v)        { this.parentId = v; }

    public List<Integer> getParentIds()       { return parentIds; }
    public void setParentIds(List<Integer> v) { this.parentIds = v; }

    @ApiModelProperty("IPED processing categories")
    public Set<String> getCategories()              { return categories; }
    public void setCategories(Set<String> v)        { this.categories = v; }

    @ApiModelProperty("Bookmark names this item belongs to")
    public List<String> getBookmarks()              { return bookmarks; }
    public void setBookmarks(List<String> v)        { this.bookmarks = v; }

    @ApiModelProperty("True if the item is checked/selected in the current session")
    public boolean isSelected()               { return selected; }
    public void setSelected(boolean v)        { this.selected = v; }

    public boolean isHasChildren()            { return hasChildren; }
    public void setHasChildren(boolean v)     { this.hasChildren = v; }

    @ApiModelProperty("True if a viewer preview was generated for this item")
    public boolean isHasPreview()             { return hasPreview; }
    public void setHasPreview(boolean v)      { this.hasPreview = v; }

    @ApiModelProperty("All Tika/Lucene metadata fields stored for this item")
    public Map<String, List<String>> getMetadata()       { return metadata; }
    public void setMetadata(Map<String, List<String>> v) { this.metadata = v; }
}
