# ISSUE-268: Batch submit modal

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 3 — Hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The batch submit modal scans `sourceDir` children and enqueues one run per child via `POST /runs/batch`, then shows the enqueued count and a per-item error list on result.

## Problem

The backend's batch submission endpoint needed a frontend surface so operators could trigger and monitor folder-wide batch submissions.

## Acceptance criteria

- [x] Modal scans `sourceDir` children and calls `POST /runs/batch`.
- [x] Result view shows `enqueuedCount` and a per-item error list.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
