# ISSUE-234: Log streaming per run

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle completeness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`GET /run/{id}/stream` streams SSE `log` events sourced from the process stdout, and `RunStats.tailLines(20)` feeds recent log lines into the dashboard snapshot.

## Problem

Operators needed to tail engine/agent logs through the dashboard rather than shelling into the host running the process.

## Acceptance criteria

- [x] `GET /run/{id}/stream` streams SSE `log` events from process stdout.
- [x] `RunStats.tailLines(20)` feeds recent log lines into the dashboard snapshot.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
