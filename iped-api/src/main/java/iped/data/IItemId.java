package iped.data;

/**
 * Stable, globally-unique identifier for an item within a multi-source IPED session.
 *
 * <p>An item id is a (sourceId, id) pair. Within a single case {@link #getSourceId()} is always 0;
 * across a multi-source session it distinguishes items from different indexed cases.
 *
 * <p>Item ids are {@link Comparable} and ordered first by source id, then by item id, to allow
 * sorted collections across sources.
 */
public interface IItemId extends Comparable<IItemId> {

  /**
   * @return the stable, zero-based IPED item id within its source
   */
  int getId();

  /**
   * @return the numeric id of the {@link IIPEDSource} this item belongs to; {@code 0} for
   *     single-source sessions
   */
  int getSourceId();
}
