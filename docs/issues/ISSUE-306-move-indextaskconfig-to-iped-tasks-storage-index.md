# ISSUE-306: Move IndexTaskConfig to iped-tasks-storage-index and switch engine call sites

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 1 — Finish config/code ownership moves
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`IndexTaskConfig.java` and `IndexTaskConfig.toml` were moved to
`iped-tasks-storage-index`, implementing the new `IndexSettings` interface
(ISSUE-305), and the four engine call sites were switched to the
`instanceof`-based lookup so the engine no longer needs the concrete class.

## Problem

Completing this move required coordinating the new `IndexSettings`
interface/lookup mechanism (ISSUE-305) with the actual file relocation and updating
every engine-side consumer, while keeping `iped-app`'s `MenuClass` and
`iped-tasks-storage-index`'s own `IndexTask`/`ElasticSearchIndexTask` working,
since those need the concrete class for `isStoreTermVectors()` (not part of the
engine-facing interface).

## Acceptance criteria

- [x] `IndexTaskConfig.java` + `IndexTaskConfig.toml` moved to
      `iped-tasks-storage-index`, implementing `IndexSettings`.
- [x] The four engine call sites switched to
      `findObjectInstanceOf(IndexSettings.class)`.
- [x] `iped-app`'s `MenuClass` and `iped-tasks-storage-index`'s own
      `IndexTask`/`ElasticSearchIndexTask` continue referencing the concrete
      `IndexTaskConfig` directly for `isStoreTermVectors()` (no cycle, since both
      already depended on `iped-tasks-storage-index`).
- [x] Verified `iped-engine-core`, `iped-engine`, `iped-tasks-storage-index` test
      suites green; `iped-app` compiles.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
