package iped.engine.webapi.spi;

import java.util.List;

public interface SearchService {

  /**
   * Legacy v1 search — returns all matching (sourceId, itemId) pairs with no pagination. Prefer
   * {@link #searchPaginated} for new callers.
   */
  List<DocRef> search(String query, String sourceId) throws Exception;

  /**
   * Paginated search returning rich item metadata for each result.
   *
   * @param query Lucene query string
   * @param sourceId source to restrict the search to, or {@code null}/{@code ""} for all sources
   * @param offset zero-based start index (must be ≥ 0)
   * @param limit maximum number of items to return (must be 1–1000)
   * @return a {@link SearchPage} with metadata-enriched items and total count
   */
  SearchPage searchPaginated(String query, String sourceId, int offset, int limit) throws Exception;
}
