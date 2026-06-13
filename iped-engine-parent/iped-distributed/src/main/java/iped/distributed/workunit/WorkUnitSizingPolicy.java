package iped.distributed.workunit;

import iped.distributed.config.DistributedConfig;

/**
 * Tunable thresholds that govern how {@link AdaptiveWorkUnitPlanner} groups items
 * into work units.  All sizes are in <b>weighted bytes</b> (raw byte length scaled by
 * the {@link MediaCostModel} cost weight), except where noted.
 *
 * @param targetUnitBytes    soft fill target — a unit accumulates items until adding the
 *                           next would push its weighted size past this value, then it is
 *                           flushed.  Controls the typical batch size.
 * @param maxUnitItems       hard cap on the number of items in a single unit.  Prevents a
 *                           flood of tiny/zero-length files from forming an unboundedly
 *                           large unit (where weighted bytes alone would never trigger a flush).
 * @param oversizedItemBytes a single item whose weighted size reaches this threshold is
 *                           isolated into its own unit (flagged {@link WorkUnit#oversized()}),
 *                           so one huge or very expensive item never shares a unit and never
 *                           creates load imbalance.  Must be ≥ {@code targetUnitBytes}.
 */
public record WorkUnitSizingPolicy(
        long targetUnitBytes,
        int  maxUnitItems,
        long oversizedItemBytes
) {

    public WorkUnitSizingPolicy {
        if (targetUnitBytes <= 0)
            throw new IllegalArgumentException("targetUnitBytes must be > 0");
        if (maxUnitItems <= 0)
            throw new IllegalArgumentException("maxUnitItems must be > 0");
        if (oversizedItemBytes < targetUnitBytes)
            throw new IllegalArgumentException(
                    "oversizedItemBytes (" + oversizedItemBytes + ") must be >= targetUnitBytes ("
                            + targetUnitBytes + ")");
    }

    /** 64 MB target, 256 items max, 128 MB isolation threshold. */
    public static WorkUnitSizingPolicy defaults() {
        return new WorkUnitSizingPolicy(64L * 1024 * 1024, 256, 128L * 1024 * 1024);
    }

    /** Builds a policy from the distributed configuration's {@code workUnit*} settings. */
    public static WorkUnitSizingPolicy fromConfig(DistributedConfig cfg) {
        return new WorkUnitSizingPolicy(
                cfg.getWorkUnitTargetBytes(),
                cfg.getWorkUnitMaxItems(),
                cfg.getWorkUnitOversizedBytes());
    }
}
