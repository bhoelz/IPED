package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/** Maps {@code GET /v2/sources/{src}/items/{id}} — ItemMetadataJSON from iped-webapi. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemMetadataDto {
  private String itemId;
  private String name;
  private String path;
  private String mediaType;
  private Long size;
  private String hash;
  private String modDate;
  private String creationDate;
  private Boolean deleted;
  private Boolean carved;
  private List<String> bookmarks;
  private Map<String, Object> metadata;

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String v) {
    itemId = v;
  }

  public String getName() {
    return name;
  }

  public void setName(String v) {
    name = v;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String v) {
    path = v;
  }

  public String getMediaType() {
    return mediaType;
  }

  public void setMediaType(String v) {
    mediaType = v;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long v) {
    size = v;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String v) {
    hash = v;
  }

  public String getModDate() {
    return modDate;
  }

  public void setModDate(String v) {
    modDate = v;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(String v) {
    creationDate = v;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean v) {
    deleted = v;
  }

  public Boolean getCarved() {
    return carved;
  }

  public void setCarved(Boolean v) {
    carved = v;
  }

  public List<String> getBookmarks() {
    return bookmarks;
  }

  public void setBookmarks(List<String> v) {
    bookmarks = v;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> v) {
    metadata = v;
  }
}
