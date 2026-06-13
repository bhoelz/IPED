package iped.search;

import iped.data.IItemReader;

import java.io.Closeable;
import java.util.List;

/**
 * Simplified search interface for use by processing tasks and scripts that
 * need to query the case index during processing.
 *
 * <p>Unlike {@link IIPEDSearcher}, this interface is deliberately narrow: it
 * accepts a Lucene query string and returns item readers directly, without
 * exposing Lucene internals. It is safe to use from tasks and parsers.
 *
 * <p>Callers are responsible for closing the searcher when done.
 *
 * @see IIPEDSearcher for full-featured search with scoring and multi-source support
 */
public interface IItemSearcher extends Closeable {

    /**
     * Executes a Lucene query string and returns all matching items.
     *
     * @param luceneQuery Lucene query string (same syntax as the IPED search bar)
     * @return list of matching items; empty if no matches or the index is empty
     */
    List<IItemReader> search(String luceneQuery);

    /**
     * Executes a Lucene query string and returns matches as a lazy iterable.
     * Prefer this over {@link #search(String)} for large result sets to avoid
     * loading all results into memory at once.
     *
     * @param luceneQuery Lucene query string
     * @return iterable of matching items
     */
    Iterable<IItemReader> searchIterable(String luceneQuery);

    /**
     * Escapes special Lucene characters in {@code string} so it can be used as
     * a literal term inside a larger query expression.
     *
     * @param string raw string to escape
     * @return escaped string safe for embedding in a Lucene query
     */
    String escapeQuery(String string);
}
