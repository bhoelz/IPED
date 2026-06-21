# ISSUE-317: Move CarverTask static carverTypes table into a per-case CarverAccumulator

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`CarverTask`'s static `carverTypes[]` signature table (flagged as `GLOBAL` in the
classification matrix, ISSUE-312) was moved into a per-case `CarverAccumulator`
stored in `caseData`, fixing a real bug where a batch run's second case could
silently skip carving because the static field was already non-null from the
first case.

## Problem

Previously, case 2's signature table in a batch run could be silently skipped
because the static field was already non-null from case 1.

## Acceptance criteria

- [x] `carverTypes[]` moved into a per-case `CarverAccumulator` stored in
      `caseData`.
- [x] `carverConfig`/`ledCarved`/`itensCarved` confirmed to remain static in
      `BaseCarveTask` (`iped-engine`, shared with `LedCarveTask`/
      `KnownMetCarveTask`) — explicitly out of scope here, tracked as the
      follow-up handled in ISSUE-318.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
