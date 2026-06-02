package iped.search;

import iped.data.IIPEDSource;
import iped.data.IItemId;
import iped.data.IItemReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Compile-check tests for search interfaces. Each test verifies that the
 * interface is implementable and exercises any accessible constants.
 */
class SearchInterfaceCompileChecksTest {

    // ── IIPEDSearcher ────────────────────────────────────────────────────────

    @Test
    void iIPEDSearcher_maxSizeToScore_equalsOneMillion() {
        assertEquals(1_000_000, IIPEDSearcher.MAX_SIZE_TO_SCORE);
    }

    @Test
    void iIPEDSearcher_isImplementable() throws Exception {
        IIPEDSearcher searcher = new IIPEDSearcher() {
            @Override public void cancel() {}
            @Override public Object getQueryObject() { return null; }
            @Override public SearchQueryDefinition getQueryDefinition() { return null; }
            @Override public IMultiSearchResult multiSearch() { return null; }
            @Override public SearchResult search() { return null; }
            @Override public void setQueryObject(Object q) {}
            @Override public void setQueryDefinition(SearchQueryDefinition d) {}
            @Override public void setTreeQuery(boolean t) {}
        };

        assertNull(searcher.getQueryObject());
        assertNull(searcher.getQueryDefinition());
    }

    // ── IItemSearcher ────────────────────────────────────────────────────────

    @Test
    void iItemSearcher_isImplementable() throws Exception {
        IItemSearcher searcher = new IItemSearcher() {
            @Override public List<IItemReader> search(String q) { return List.of(); }
            @Override public Iterable<IItemReader> searchIterable(String q) { return List.of(); }
            @Override public String escapeQuery(String s) { return s; }
            @Override public void close() {}
        };

        assertEquals(List.of(), searcher.search("*:*"));
        assertEquals("safe+query", searcher.escapeQuery("safe+query"));
    }

    // ── IMultiSearchResult ───────────────────────────────────────────────────

    @Test
    void iMultiSearchResult_isImplementable() {
        IMultiSearchResult result = new IMultiSearchResult() {
            @Override public IItemId getItem(int i) { return null; }
            @Override public IIPEDSource getIPEDSource() { return null; }
            @Override public Iterable<IItemId> getIterator() { return List.of(); }
            @Override public int getLength() { return 0; }
            @Override public float getScore(int i) { return 0f; }
        };

        assertEquals(0, result.getLength());
        assertEquals(0f, result.getScore(0));
        assertNull(result.getItem(0));
        assertNull(result.getIPEDSource());
    }
}
