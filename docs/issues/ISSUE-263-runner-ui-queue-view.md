# ISSUE-263: Queue view with priorities and cancel

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 2 — Launch and queue management
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`QueuePanel` lists pending runs with a priority badge, profile, enqueue time, and a per-row cancel button that calls `DELETE /v2/jobs/{id}`.

## Problem

Operators needed visibility into queued (not-yet-started) runs and the ability to cancel them before they start.

## Acceptance criteria

- [x] `QueuePanel` shows priority badge, profile, and enqueue time per queued run.
- [x] Per-row cancel button calls `DELETE /v2/jobs/{id}`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
