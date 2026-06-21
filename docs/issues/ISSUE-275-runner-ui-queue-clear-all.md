# ISSUE-275: Queue clear-all action

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 4 — Convergence (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A "Clear queue" button in the queue panel header requires a confirm click (first click shows "Confirm?", second click fires the action), then issues parallel `DELETE /v2/jobs/{id}` for all queued items and refreshes jobs and supporting data.

## Problem

Clearing a large backlog of queued runs one at a time was tedious; operators needed a bulk action with a safety confirmation.

## Acceptance criteria

- [x] "Clear queue" button requires a confirm step before acting.
- [x] Issues parallel `DELETE /v2/jobs/{id}` for all queued items.
- [x] Refreshes jobs and supporting data after clearing.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
