package iped.search;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SearchResultTest {

  @Test
  void getters_whenInitialized_thenReturnExpectedValues() {
    int[] ids = {10, 20};
    float[] scores = {1.5f, 2.5f};
    SearchResult result = new SearchResult(ids, scores);

    ids[0] = 99;
    scores[1] = 9.5f;

    assertEquals(10, result.getId(0));
    assertEquals(2.5f, result.getScore(1));
    assertEquals(2, result.getLength());
    assertArrayEquals(new int[] {10, 20}, result.getIds());
  }

  @Test
  void getIds_whenCalled_thenReturnsDefensiveCopy() {
    SearchResult result = new SearchResult(new int[] {10, 20}, new float[] {1.5f, 2.5f});
    int[] ids = result.getIds();

    ids[0] = 99;

    assertEquals(10, result.getId(0));
  }

  @Test
  void compactResults_whenContainsDeletedEntries_thenCompactsKeepingOrder() {
    SearchResult result =
        new SearchResult(new int[] {5, -1, 7, -1, 9}, new float[] {1f, 2f, 3f, 4f, 5f});

    result.compactResults();

    assertArrayEquals(new int[] {5, 7, 9}, result.getIds());
    assertEquals(3, result.getLength());
    assertEquals(1f, result.getScore(0));
    assertEquals(3f, result.getScore(1));
    assertEquals(5f, result.getScore(2));
  }

  @Test
  void compactResults_whenAllEntriesDeleted_thenBecomesEmpty() {
    SearchResult result = new SearchResult(new int[] {-1, -1}, new float[] {1f, 2f});

    result.compactResults();

    assertEquals(0, result.getLength());
    assertArrayEquals(new int[0], result.getIds());
  }

  @Test
  void copy_whenCalled_thenCreatesDeepCopy() {
    SearchResult result = new SearchResult(new int[] {1, -1, 2}, new float[] {3f, 4f, 5f});

    SearchResult cloned = result.copy();
    result.compactResults();

    assertNotSame(result, cloned);
    assertEquals(1, cloned.getId(0));
    assertEquals(3f, cloned.getScore(0));
    assertEquals(3, cloned.getLength());
  }
}
