# ISSUE-316: Move VideoThumbTask static state into a per-case VideoThumbAccumulator

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`VideoThumbTask`'s static counters and `processedVideos` cache (flagged as
`GLOBAL` in the classification matrix, ISSUE-312) were moved into a per-case
`VideoThumbAccumulator`.

## Problem

Static counters and a static reuse cache were not reset between cases, risking
incorrect aggregate stats and stale cache hits across a batch run.

## Acceptance criteria

- [x] `finished`, the six processing counters
      (`totalVideosProcessed/Failed/Time`,
      `totalAnimatedImagesProcessed/Failed/Time`, `totalTimeGallery`,
      `totalGallery`), and the `processedVideos` reuse cache moved into a
      per-case `VideoThumbAccumulator` stored in `caseData`.
- [x] `taskEnabled`/`mplayer`/`init` intentionally left static (one-time MPlayer
      detection, not case data).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
