# ISSUE-064: Adaptive work-unit sizing strategy

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 3 — Scale and scheduling
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Size work units adaptively based on evidence type/size (weighted by a per-media-type cost model) instead of using fixed splits, so load is balanced across agents.

## Problem

Fixed-size segment splits ignore the real processing cost of different evidence types (videos and disk images cost far more to process per byte than empty directories), leading to load skew across agents.

## Acceptance criteria

- [x] `iped.distributed.workunit.AdaptiveWorkUnitPlanner` packs items greedily by weighted size (raw bytes × `MediaCostModel` per-type cost: videos 4x, disk images 3.5x, archives 3x, images 1.5x, dirs/empty 0.1x).
- [x] `WorkUnitSizingPolicy` (soft `targetUnitBytes`=64MB, hard `maxUnitItems`=256, `oversizedItemBytes`=128MB isolation).
- [x] A single huge/expensive item is isolated into its own `oversized` unit.
- [x] Plan is a pure, deterministic function of (input order, policy, cost model), preserving the exactly-once-effect guarantee.
- [x] Config knobs `workUnit*` in `DistributedConfig`; documented in `specs/90-adaptive-work-unit-sizing.md`; 22 broker-free tests in `AdaptiveWorkUnitPlannerTest`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 3), status set to `done`. Producer/agent wiring to dispatch a `WorkUnit` as a compound message is tracked separately (see ISSUE-071, work-unit producer wiring).
