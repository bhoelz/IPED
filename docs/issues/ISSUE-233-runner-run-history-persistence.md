# ISSUE-233: Run history persistence across runner restarts

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 1 — Run lifecycle completeness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Run history now survives runner restarts via a JSON file (`run-history.json`), with `RunHistoryService` persisting after every terminal event and reloading on startup.

## Problem

Run state was previously in-memory/topic-derived only, so a runner restart lost history. A decision was needed between a small embedded DB, a compacted topic, or simpler file persistence.

## Acceptance criteria

- [x] Decision recorded: JSON file persistence (`run-history.json`), configurable via `runner.history-file`.
- [x] `RunHistoryService` persists after every terminal event and loads on startup.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
