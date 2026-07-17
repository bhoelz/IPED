package iped.distributed.workunit;

import java.util.List;

/**
 * A planned group of items to be dispatched as a single distributed work unit.
 *
 * <p>Produced by {@link AdaptiveWorkUnitPlanner}. Carries the item identifiers in the unit plus
 * aggregate statistics used for scheduling and observability.
 *
 * @param index zero-based position of this unit within the plan
 * @param itemUuids UUIDs of the items in this unit, in input order
 * @param totalBytes sum of the items' raw byte lengths
 * @param weightedBytes sum of the items' {@link MediaCostModel}-weighted sizes; the value the
 *     planner actually packs against
 * @param oversized true when this unit holds a single item that alone met the policy's {@code
 *     oversizedItemBytes} isolation threshold
 */
public record WorkUnit(
    int index, List<String> itemUuids, long totalBytes, double weightedBytes, boolean oversized) {
  public int itemCount() {
    return itemUuids.size();
  }
}
