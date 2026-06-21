# ISSUE-261: Auto-refresh via SSE/polling instead of manual refresh

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle UX
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Log tail uses SSE; the job list, queue, agents, and metrics panels poll every 3 seconds via `setInterval`, since the list endpoints have no SSE transport and polling is the appropriate approach there.

## Problem

The dashboard needed to stay current without requiring a manual page refresh.

## Acceptance criteria

- [x] Log tail uses SSE for real-time updates.
- [x] Job list / queue / agents / metrics poll on a 3s interval.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
