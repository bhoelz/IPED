package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DocGroupDto {
    private String source;
    private List<Integer> ids;

    public DocGroupDto() {
    }

    public DocGroupDto(String source, List<Integer> ids) {
        this.source = source;
        this.ids = ids;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }
}
