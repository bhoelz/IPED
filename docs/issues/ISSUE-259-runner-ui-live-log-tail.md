# ISSUE-259: Live log tail per run via SSE

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle UX
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`JobDetail` opens an SSE `EventSource` on `/run/{id}/stream` whenever the selected job is running, auto-scrolls the log view, and closes the connection cleanly on done/error/aborted.

## Problem

Operators needed to watch live log output for an in-progress run from the dashboard.

## Acceptance criteria

- [x] SSE `EventSource` opens on `/run/{id}/stream` for running jobs.
- [x] Log view auto-scrolls.
- [x] Connection closes cleanly on done/error/aborted.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
