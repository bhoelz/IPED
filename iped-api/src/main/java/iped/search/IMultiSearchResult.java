package iped.search;

import iped.data.IIPEDSource;
import iped.data.IItemId;

/**
 * A search result set that may span multiple indexed IPED sources.
 *
 * <p>Items are identified by {@link IItemId} instances, which combine a
 * source id with an item id, allowing results from different cases to coexist
 * in the same result set.
 *
 * <p>The result is ordered: item at index {@code i} corresponds to score at
 * index {@code i} from {@link #getScore(int)}.
 */
public interface IMultiSearchResult {

    /**
     * Returns the item identifier at position {@code i} in the result set.
     *
     * @param i zero-based result index
     * @return the item id at that position
     */
    IItemId getItem(int i);

    /**
     * @return the primary IPED source associated with this result set;
     *         when multiple sources contribute, this is the "anchor" source
     */
    IIPEDSource getIPEDSource();

    /**
     * @return iterable over all item ids in this result set, in result order
     */
    Iterable<IItemId> getIterator();

    /**
     * @return total number of items in this result set
     */
    int getLength();

    /**
     * Returns the relevance score of the item at position {@code i}.
     * Returns {@code 0.0f} when scoring was skipped (result set larger than
     * {@link IIPEDSearcher#MAX_SIZE_TO_SCORE}).
     *
     * @param i zero-based result index
     * @return relevance score in the range {@code [0.0, ∞)}
     */
    float getScore(int i);
}
