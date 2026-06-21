# ISSUE-258: Run detail view with full state machine and metadata

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle UX
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`DashboardView`'s `JobDetail` panel shows the full run state machine (queued/running/completed/failed/timed-out) with a status chip, progress bar, timing/source/items metadata grid, and failure message display.

## Problem

Operators needed a detailed per-run view reflecting the backend's full lifecycle states, not just a coarse status indicator.

## Acceptance criteria

- [x] `JobDetail` panel shows status chip and progress bar.
- [x] Metadata grid covers started/ended/duration/source/items.
- [x] Failure message is displayed when applicable.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
