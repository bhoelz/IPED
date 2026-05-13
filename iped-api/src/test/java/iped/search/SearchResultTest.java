package iped.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class SearchResultTest {

    @Test
    void getters_whenInitialized_thenReturnExpectedValues() {
        SearchResult result = new SearchResult(new int[] { 10, 20 }, new float[] { 1.5f, 2.5f });

        assertEquals(10, result.getId(0));
        assertEquals(2.5f, result.getScore(1));
        assertEquals(2, result.getLength());
        assertArrayEquals(new int[] { 10, 20 }, result.getIds());
    }

    @Test
    void compactResults_whenContainsDeletedEntries_thenCompactsKeepingOrder() {
        SearchResult result = new SearchResult(new int[] { 5, -1, 7, -1, 9 }, new float[] { 1f, 2f, 3f, 4f, 5f });

        result.compactResults();

        assertArrayEquals(new int[] { 5, 7, 9 }, result.getIds());
        assertEquals(3, result.getLength());
        assertEquals(1f, result.getScore(0));
        assertEquals(3f, result.getScore(1));
        assertEquals(5f, result.getScore(2));
    }

    @Test
    void compactResults_whenAllEntriesDeleted_thenBecomesEmpty() {
        SearchResult result = new SearchResult(new int[] { -1, -1 }, new float[] { 1f, 2f });

        result.compactResults();

        assertEquals(0, result.getLength());
        assertArrayEquals(new int[0], result.getIds());
    }

    @Test
    void clone_whenCalled_thenCreatesDeepCopy() {
        SearchResult result = new SearchResult(new int[] { 1, 2 }, new float[] { 3f, 4f });

        SearchResult cloned = result.clone();
        result.getIds()[0] = 99;

        assertNotSame(result, cloned);
        assertEquals(1, cloned.getId(0));
        assertEquals(3f, cloned.getScore(0));
    }
}

