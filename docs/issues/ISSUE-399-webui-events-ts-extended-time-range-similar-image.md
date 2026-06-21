# ISSUE-399: events.ts extended with time-range and similar-image events

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 2 — Island buildout
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`events.ts` gained `timeRangeSelectedEvent` and `similarImageSearchEvent`; `ItemSelectedDetail.itemId` changed from `number` to `string` (composite `{sourceId}:{docId}` form), and `SelectionChangedDetail` now uses `string[]`. Call sites in results-grid were updated accordingly.

## Problem

The event contract needed extension for the timeline and gallery islands' new interactions, and item identity needed to support composite source+doc IDs across islands.

## Acceptance criteria

- [x] `timeRangeSelectedEvent` and `similarImageSearchEvent` added to `events.ts`.
- [x] `ItemSelectedDetail.itemId` is `string` (composite `{sourceId}:{docId}`).
- [x] `SelectionChangedDetail` uses `string[]`.
- [x] Results-grid call sites updated to match the new types.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
