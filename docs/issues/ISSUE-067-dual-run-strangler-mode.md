# ISSUE-067: Dual-run mode (strangler pattern) for cutover validation

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 4 — Production rollout (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Process the same case through both the monolithic and distributed paths, automatically diff the results, and use the comparison as the exit criterion for cutover to the distributed pipeline.

## Problem

Before cutting production cases over to the distributed pipeline, there needed to be a safe way to verify the distributed path produces identical results to the existing monolithic pipeline, without manually comparing outputs.

## Acceptance criteria

- [x] New `iped.distributed.dualrun` package: `ItemSummary`, `DualRunVerdict` (INCOMPLETE/MATCH/MISMATCH), `AttributeMismatch`, `DualRunReport`, `DualRunComparator` (stateless, path-based matching, list capped at 500), `DualRunSession` (per-case thread-safe accumulator), `DualRunManager` (coordinator-side session map).
- [x] `ItemStatusEvent` enriched with `mediaType`/`lengthBytes` (non-breaking, backward-compatible deserialization).
- [x] `DistributedConfig.dualRunEnabled` (default false): when true, a session is auto-opened for every new case.
- [x] Three REST endpoints: `POST /api/v1/dualrun/{caseId}/start`, `POST /api/v1/dualrun/{caseId}/reference`, `GET /api/v1/dualrun/{caseId}`.
- [x] 25 tests in `DualRunComparatorTest` cover INCOMPLETE/MATCH/MISMATCH paths, attribute comparison, session lifecycle, and manager routing.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 4), status set to `done`.
