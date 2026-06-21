# ISSUE-273: Runs list filter/search

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 4 — Convergence (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A status-chip filter (all/running/pending/completed/failed/cancelled) plus a name/ID text search sit above the job list panel; `filteredJobs` is derived via `useMemo` and auto-deselects the selected job when it is filtered out.

## Problem

As run history grew, operators needed to narrow the job list by status or search by name/ID.

## Acceptance criteria

- [x] Status-chip filter covers all/running/pending/completed/failed/cancelled.
- [x] Text search filters by name/ID.
- [x] Selected job auto-deselects when filtered out.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
