# ISSUE-303: Move VideoThumbsConfig ownership to iped-tasks-image

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 1 — Finish config/code ownership moves
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`VideoThumbsConfig` and its `VideoThumbsConfig.toml` were relocated from
`iped-engine` to `iped-tasks-image`, continuing the task-owned configuration move.

## Problem

Video-thumbnail task configuration lived in the engine instead of the
`iped-tasks-image` module that actually consumes it.

## Acceptance criteria

- [x] `VideoThumbsConfig` class moved to `iped-tasks-image`.
- [x] `VideoThumbsConfig.toml` moved alongside it.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
