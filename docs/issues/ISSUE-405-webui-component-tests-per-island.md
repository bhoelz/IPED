# ISSUE-405: Component tests per island

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 4 — Quality bar
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`results-grid`, `hex-viewer`, `timeline`, `gallery`, and `graph` component spec files use `TestBed` + `HttpTestingController` to cover initial/empty state, HTTP flow, `CustomEvent` dispatch for output events, host-event pagination, error states, and demo-data fallback.

## Problem

Islands had no automated test coverage for attribute changes, event emission, or error states, risking regressions as islands evolved independently.

## Acceptance criteria

- [x] Each of results-grid, hex-viewer, timeline, gallery, graph has a `.spec.ts` test file.
- [x] Tests cover initial/empty state, HTTP flow, CustomEvent dispatch, error states.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
