package iped.distributed.workunit;

import iped.distributed.kafka.KafkaItemMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups raw items into adaptively-sized work units instead of emitting one fixed-size
 * work unit per item.
 *
 * <h2>Why adaptive</h2>
 * <p>A forensic case is a mix of extremes: millions of tiny files (registry keys, cached
 * thumbnails, log lines) alongside a handful of multi-gigabyte disk images and videos.
 * Dispatching one work unit per item wastes throughput on the small files (per-message,
 * per-poll, per-commit overhead dominates) while dispatching a <i>fixed</i> count per unit
 * risks bundling several huge items together, producing units that blow an agent's memory
 * budget and skew load across the pool.
 *
 * <p>This planner packs items greedily by <b>weighted size</b> — raw bytes scaled by a
 * {@link MediaCostModel} per-type cost — under a {@link WorkUnitSizingPolicy}:
 * <ul>
 *   <li>cheap, small items accumulate until the unit reaches {@code targetUnitBytes}
 *       or {@code maxUnitItems};</li>
 *   <li>a single item at or above {@code oversizedItemBytes} is isolated into its own
 *       unit (never sharing, never co-located with others);</li>
 *   <li>expensive types (archives, videos, disk images) reach the target with fewer
 *       items because their weight is higher.</li>
 * </ul>
 *
 * <h2>Determinism</h2>
 * <p>The plan is a pure function of the input order, the policy, and the cost model:
 * the same input always yields the same units.  This keeps work-unit identity stable
 * across reader restarts and is what allows the rest of the pipeline (deterministic
 * sub-item UUIDs, idempotent indexing) to keep its exactly-once-effect guarantee.
 *
 * <p>The planner is stateless and therefore thread-safe; a single instance may be shared.
 */
@Slf4j
public class AdaptiveWorkUnitPlanner {

    private final WorkUnitSizingPolicy policy;
    private final MediaCostModel costModel;

    public AdaptiveWorkUnitPlanner() {
        this(WorkUnitSizingPolicy.defaults(), new MediaCostModel());
    }

    public AdaptiveWorkUnitPlanner(WorkUnitSizingPolicy policy, MediaCostModel costModel) {
        this.policy = policy;
        this.costModel = costModel;
    }

    /**
     * Groups the given items into work units according to the policy.  Null items are
     * skipped.  The returned list preserves input order: a unit's items are contiguous
     * in the input, and units are numbered in the order they are flushed.
     *
     * @param items raw items (typically stage-0 output of a datasource reader)
     * @return the planned work units (empty if no non-null items were given)
     */
    public List<WorkUnit> plan(List<KafkaItemMessage> items) {
        List<WorkUnit> units = new ArrayList<>();
        if (items == null || items.isEmpty()) return units;

        Accumulator current = new Accumulator();

        for (KafkaItemMessage item : items) {
            if (item == null) continue;

            long bytes = item.getLength() != null ? Math.max(0L, item.getLength()) : 0L;
            double weighted = bytes * costModel.weight(item);

            // Isolate an oversized item into its own unit.
            if (weighted >= policy.oversizedItemBytes()) {
                if (!current.isEmpty()) units.add(current.flush(units.size(), false));
                current.add(item.getItemUuid(), bytes, weighted);
                units.add(current.flush(units.size(), true));
                continue;
            }

            // Would adding this item exceed the soft byte target or the hard item cap?
            boolean wouldExceedBytes = !current.isEmpty()
                    && current.weightedBytes + weighted > policy.targetUnitBytes();
            boolean wouldExceedItems = current.count >= policy.maxUnitItems();
            if (wouldExceedBytes || wouldExceedItems) {
                units.add(current.flush(units.size(), false));
            }

            current.add(item.getItemUuid(), bytes, weighted);
        }

        if (!current.isEmpty()) units.add(current.flush(units.size(), false));

        if (log.isDebugEnabled()) {
            log.debug("Planned {} work unit(s) from {} item(s)", units.size(), items.size());
        }
        return units;
    }

    // -----------------------------------------------------------------------

    /** Mutable per-unit accumulator, reused across flushes. */
    private static final class Accumulator {
        List<String> uuids = new ArrayList<>();
        long totalBytes;
        double weightedBytes;
        int count;

        boolean isEmpty() { return count == 0; }

        void add(String uuid, long bytes, double weighted) {
            uuids.add(uuid);
            totalBytes += bytes;
            weightedBytes += weighted;
            count++;
        }

        WorkUnit flush(int index, boolean oversized) {
            WorkUnit unit = new WorkUnit(index, List.copyOf(uuids), totalBytes, weightedBytes, oversized);
            uuids = new ArrayList<>();
            totalBytes = 0;
            weightedBytes = 0;
            count = 0;
            return unit;
        }
    }
}
