package iped.engine.webapi.spi;

import java.util.Date;
import java.util.Set;

/**
 * A single item in a paginated search result, carrying the metadata fields most
 * useful to a browser UI without requiring a second round-trip to the item endpoint.
 */
public class SearchResultItem {

    private final String sourceId;
    private final int id;
    private final String name;
    private final String path;
    private final String mediaType;
    private final Long size;
    private final String hash;
    private final Date modDate;
    private final Date creationDate;
    private final boolean deleted;
    private final boolean dir;
    private final Set<String> categories;

    public SearchResultItem(String sourceId, int id, String name, String path,
            String mediaType, Long size, String hash,
            Date modDate, Date creationDate,
            boolean deleted, boolean dir, Set<String> categories) {
        this.sourceId     = sourceId;
        this.id           = id;
        this.name         = name;
        this.path         = path;
        this.mediaType    = mediaType;
        this.size         = size;
        this.hash         = hash;
        this.modDate      = modDate;
        this.creationDate = creationDate;
        this.deleted      = deleted;
        this.dir          = dir;
        this.categories   = categories;
    }

    public String getSourceId()     { return sourceId; }
    public int getId()              { return id; }
    public String getName()         { return name; }
    public String getPath()         { return path; }
    public String getMediaType()    { return mediaType; }
    public Long getSize()           { return size; }
    public String getHash()         { return hash; }
    public Date getModDate()        { return modDate; }
    public Date getCreationDate()   { return creationDate; }
    public boolean isDeleted()      { return deleted; }
    public boolean isDir()          { return dir; }
    public Set<String> getCategories() { return categories; }
}
