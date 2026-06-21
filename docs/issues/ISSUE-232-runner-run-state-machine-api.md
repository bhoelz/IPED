# ISSUE-232: Full run state machine surfaced in the API

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle completeness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The runner API surfaces the complete run lifecycle (queued, running, completed, failed, aborted, timed-out) backed by the `RunStatus` enum, with live and history-backed status available through dedicated endpoints.

## Problem

Callers needed a single accurate source of truth for a run's state across its full lifecycle, including terminal states detected by the distributed module.

## Acceptance criteria

- [x] `RunStatus` enum covers QUEUED/RUNNING/COMPLETED/FAILED/ABORTED/TIMED_OUT.
- [x] `pumpOutput` sets COMPLETED or FAILED on process exit.
- [x] `abort()` uses SIGTERM, then a configurable wait, then SIGKILL.
- [x] `GET /run/{id}` returns live status for active runs and history-backed status for terminal runs.
- [x] `GET /runs` returns all history.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
