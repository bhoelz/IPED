package iped.search;

import lombok.Getter;

/**
 * The result of a search over a single case: parallel arrays of item ids and
 * their relevance scores.
 */
public class SearchResult {

    @Getter
    int[] ids;
    float[] scores;

    private SearchResult() {
    }

    /**
     * Creates a result over the given parallel arrays, which are kept by
     * reference, not copied.
     *
     * @param ids    item ids
     * @param scores relevance score of each id, in the same order
     */
    public SearchResult(int[] ids, float[] scores) {
        this.ids = ids;
        this.scores = scores;
    }

    /**
     * @param i index into the result
     * @return the item id at the given index
     */
    public int getId(int i) {
        return ids[i];
    }

    /**
     * @param i index into the result
     * @return the relevance score at the given index
     */
    public float getScore(int i) {
        return scores[i];
    }

    /**
     * @return the number of items in this result
     */
    public int getLength() {
        return ids.length;
    }

    /**
     * Removes entries whose id was marked as {@code -1}, shrinking the backing
     * arrays in place.
     */
    public void compactResults() {
        int blanks = 0;
        for (int i = 0; i < ids.length; i++)
            if (ids[i] != -1) {
                ids[i - blanks] = ids[i];
                scores[i - blanks] = scores[i];
            } else
                blanks++;

        int[] _ids = new int[ids.length - blanks];
        float[] _scores = new float[scores.length - blanks];

        System.arraycopy(ids, 0, _ids, 0, _ids.length);
        System.arraycopy(scores, 0, _scores, 0, _scores.length);

        ids = _ids;
        scores = _scores;
    }

    /**
     * @return a deep copy of this result, with its own id and score arrays
     */
    @Override
    public SearchResult clone() {
        SearchResult result = new SearchResult();
        result.ids = this.ids.clone();
        result.scores = this.scores.clone();
        return result;
    }
}
