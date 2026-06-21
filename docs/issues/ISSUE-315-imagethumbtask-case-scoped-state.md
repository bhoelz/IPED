# ISSUE-315: Move ImageThumbTask static state into a per-case ImageThumbAccumulator

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`ImageThumbTask`'s static `performanceStatsPerType` map (flagged as `GLOBAL` and
never cleared between cases in the classification matrix, ISSUE-312) was moved into
a per-case `ImageThumbAccumulator`.

## Problem

`performanceStatsPerType` was a static map never cleared between cases, risking
stale performance stats bleeding across a batch run.

## Acceptance criteria

- [x] `performanceStatsPerType`, `logInit`, `finished` moved into a per-case
      `ImageThumbAccumulator` stored in `caseData`.
- [x] `executor` (thread pool) and `extConvPropInit` (one-time system-property
      setup) intentionally left static as process-wide resources.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
