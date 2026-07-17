package iped.engine.webapi.spi;

import java.util.List;

/** Paginated search result returned by {@link SearchService#searchPaginated}. */
public class SearchPage {

  private final List<SearchResultItem> items;
  private final long total;
  private final int offset;
  private final int limit;

  public SearchPage(List<SearchResultItem> items, long total, int offset, int limit) {
    this.items = items;
    this.total = total;
    this.offset = offset;
    this.limit = limit;
  }

  /** Items in this page. */
  public List<SearchResultItem> getItems() {
    return items;
  }

  /** Total number of results matching the query (before pagination). */
  public long getTotal() {
    return total;
  }

  /** Zero-based start index of this page. */
  public int getOffset() {
    return offset;
  }

  /** Maximum number of items requested per page. */
  public int getLimit() {
    return limit;
  }
}
