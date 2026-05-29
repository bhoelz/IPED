package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DocPropsDto {
    private String source;
    private int id;
    private int luceneId;
    private Map<String, String[]> properties;
    private List<String> bookmarks;
    private boolean selected;

    public DocPropsDto() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLuceneId() {
        return luceneId;
    }

    public void setLuceneId(int luceneId) {
        this.luceneId = luceneId;
    }

    public Map<String, String[]> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String[]> properties) {
        this.properties = properties;
    }

    public List<String> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(List<String> bookmarks) {
        this.bookmarks = bookmarks;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
