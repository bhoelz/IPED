package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResultDto {
    private List<DocGroupDto> data;

    public SearchResultDto() {
    }

    public SearchResultDto(List<DocGroupDto> data) {
        this.data = data;
    }

    public List<DocGroupDto> getData() {
        return data;
    }

    public void setData(List<DocGroupDto> data) {
        this.data = data;
    }
}
