package iped.engine.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Maps {@code GET /v2/search} — SearchResultPageJSON from iped-webapi.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchPageDto {
    private long total;
    private int offset;
    private int limit;
    private List<Map<String, Object>> items;

    public long getTotal()          { return total; }
    public void setTotal(long v)    { total = v; }

    public int getOffset()          { return offset; }
    public void setOffset(int v)    { offset = v; }

    public int getLimit()           { return limit; }
    public void setLimit(int v)     { limit = v; }

    public List<Map<String, Object>> getItems()            { return items; }
    public void setItems(List<Map<String, Object>> items)  { this.items = items; }
}
