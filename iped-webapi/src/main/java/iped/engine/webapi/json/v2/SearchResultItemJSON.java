package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.Set;

/**
 * A single search result item with metadata fields pre-populated so the browser UI can render the
 * results list without an additional round-trip per item.
 */
public class SearchResultItemJSON {

  private String sourceId;
  private int id;
  private String name;
  private String path;
  private String mediaType;
  private Long size;
  private String hash;
  private Date modDate;
  private Date creationDate;
  private boolean deleted;
  private boolean dir;
  private Set<String> categories;

  @ApiModelProperty("String identifier of the source case this item belongs to")
  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String v) {
    this.sourceId = v;
  }

  @ApiModelProperty("Internal IPED item id within the source")
  public int getId() {
    return id;
  }

  public void setId(int v) {
    this.id = v;
  }

  @ApiModelProperty("File or item name")
  public String getName() {
    return name;
  }

  public void setName(String v) {
    this.name = v;
  }

  @ApiModelProperty("Full path of the item inside the case")
  public String getPath() {
    return path;
  }

  public void setPath(String v) {
    this.path = v;
  }

  @ApiModelProperty("MIME type detected during processing")
  public String getMediaType() {
    return mediaType;
  }

  public void setMediaType(String v) {
    this.mediaType = v;
  }

  @ApiModelProperty("Size in bytes, null if unknown")
  public Long getSize() {
    return size;
  }

  public void setSize(Long v) {
    this.size = v;
  }

  @ApiModelProperty("Hash digest (typically MD5) of the item content")
  public String getHash() {
    return hash;
  }

  public void setHash(String v) {
    this.hash = v;
  }

  @ApiModelProperty("Last modification date, null if unknown")
  public Date getModDate() {
    return modDate;
  }

  public void setModDate(Date v) {
    this.modDate = v;
  }

  @ApiModelProperty("File creation date, null if unknown")
  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date v) {
    this.creationDate = v;
  }

  @ApiModelProperty("True if the item was found in deleted/unallocated space")
  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean v) {
    this.deleted = v;
  }

  @ApiModelProperty("True if this item is a directory or container")
  public boolean isDir() {
    return dir;
  }

  public void setDir(boolean v) {
    this.dir = v;
  }

  @ApiModelProperty("IPED categories assigned to this item during processing")
  public Set<String> getCategories() {
    return categories;
  }

  public void setCategories(Set<String> v) {
    this.categories = v;
  }
}
