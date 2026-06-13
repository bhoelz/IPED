package iped.search;

/**
 * Entry point for executing searches against one or more IPED case indexes.
 *
 * <p>A searcher is configured with a query (either as a raw query object or a
 * {@link SearchQueryDefinition}) and then executed via {@link #search()} or
 * {@link #multiSearch()}.  It is not thread-safe; each thread should obtain its
 * own instance.
 *
 * <p>Long-running searches may be cancelled via {@link #cancel()}.
 */
public interface IIPEDSearcher {

    /**
     * Maximum result set size at which relevance scoring is computed.
     * Above this limit results are returned without scores.
     */
    int MAX_SIZE_TO_SCORE = 1_000_000;

    /**
     * Requests cancellation of an in-progress search.
     * The behaviour after cancellation is implementation-defined; typically
     * the next call to {@link #search()} or {@link #multiSearch()} returns
     * immediately with a partial or empty result.
     */
    void cancel();

    /**
     * @return the raw Lucene query object set via {@link #setQueryObject},
     *         or {@code null} if a {@link SearchQueryDefinition} is being used
     */
    Object getQueryObject();

    /**
     * @return the structured query definition, or {@code null} if a raw query
     *         object is being used
     */
    SearchQueryDefinition getQueryDefinition();

    /**
     * Executes the search and returns results spanning all indexed sources.
     *
     * @return combined results across all sources
     * @throws Exception if the query is invalid or a search error occurs
     */
    IMultiSearchResult multiSearch() throws Exception;

    /**
     * Executes the search and returns results from the primary (single) source.
     *
     * @return search results from the primary source
     * @throws Exception if the query is invalid or a search error occurs
     */
    SearchResult search() throws Exception;

    /**
     * Sets the raw Lucene query object to use for the next search.
     * Mutually exclusive with {@link #setQueryDefinition}.
     *
     * @param queryObject the Lucene query; type is opaque to avoid a
     *                    compile-time Lucene dependency
     */
    void setQueryObject(Object queryObject);

    /**
     * Sets the structured query definition to use for the next search.
     * Mutually exclusive with {@link #setQueryObject}.
     *
     * @param queryDefinition the structured query
     */
    void setQueryDefinition(SearchQueryDefinition queryDefinition);

    /**
     * Controls whether the search should follow parent–child tree relationships.
     * When {@code true} the search traverses the item hierarchy; when
     * {@code false} (the default) only direct matches are returned.
     *
     * @param treeQuery {@code true} to enable tree-traversal mode
     */
    void setTreeQuery(boolean treeQuery);
}
