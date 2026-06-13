package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * Paginated search response returned by {@code GET /v2/search}.
 *
 * <pre>
 * {
 *   "total":  1500,
 *   "offset": 0,
 *   "limit":  50,
 *   "items":  [ { ... }, ... ]
 * }
 * </pre>
 */
public class SearchResultPageJSON {

    private long total;
    private int offset;
    private int limit;
    private List<SearchResultItemJSON> items;

    @ApiModelProperty("Total number of items matching the query (before pagination)")
    public long getTotal()                         { return total; }
    public void setTotal(long v)                   { this.total = v; }

    @ApiModelProperty("Zero-based index of the first item in this page")
    public int getOffset()                         { return offset; }
    public void setOffset(int v)                   { this.offset = v; }

    @ApiModelProperty("Maximum number of items per page that was requested")
    public int getLimit()                          { return limit; }
    public void setLimit(int v)                    { this.limit = v; }

    @ApiModelProperty("Items in this page")
    public List<SearchResultItemJSON> getItems()           { return items; }
    public void setItems(List<SearchResultItemJSON> v)     { this.items = v; }
}
