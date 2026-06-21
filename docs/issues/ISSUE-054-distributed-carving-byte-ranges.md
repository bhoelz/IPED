# ISSUE-054: Distributed carving — unallocated-space ranges as Kafka work units

- Status: blocked
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 3 — Capability growth
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Allow unallocated-space carving ranges to be distributed as Kafka work units, coordinating with the `iped-distributed` work-unit model so large unallocated-space items can be carved in parallel across the cluster rather than as one monolithic item.

## Problem

`iped.distributed.workunit.WorkUnit` (`AdaptiveWorkUnitPlanner`) currently only packs whole items by UUID into a unit — there is no concept of a sub-item byte range. Splitting a single unallocated-space item into independently-carved ranges requires new, correctness-critical logic: overlap windows so a signature spanning a chunk boundary isn't missed, and dedup so it isn't double-carved. This is a real forensic-correctness-sensitive feature, not a mechanical refactor, and is blocked pending design of the byte-range work-unit variant and merge/dedup pass.

## Acceptance criteria

- [ ] Investigate and document why whole-item `WorkUnit` packing cannot represent sub-item byte ranges (done — see Summary).
- [ ] Extend `WorkUnit` / `AdaptiveWorkUnitPlanner` with a byte-range variant (`startOffset`/`endOffset` + overlap) scoped to `UNALLOCATED`-typed items.
- [ ] Design the merge/dedup pass for ranges that overlap a signature spanning a chunk boundary.
- [ ] Implement the splitting logic once the byte-range model and merge/dedup design are finalized.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `blocked` based on the original `[ ]` marker (explicitly described as "Investigated, not yet buildable" pending new work-unit model design).
