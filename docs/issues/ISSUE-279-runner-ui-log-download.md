# ISSUE-279: Log download button

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 5 — Operator UX polish
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A "Save log" button in the `JobDetail` header (shown when `logs.length > 0`) creates a `Blob` and triggers an `<a download>` for `run-{name}-{id}.log`.

## Problem

Operators needed to save a run's log output locally for offline review or attaching to incident reports.

## Acceptance criteria

- [x] "Save log" button appears when logs are present.
- [x] Clicking it downloads `run-{name}-{id}.log` via a `Blob`/`<a download>`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
