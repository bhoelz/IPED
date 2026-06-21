# ISSUE-083: Move task-specific configuration classes to owning iped-tasks-* modules

- Status: in_progress
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 1 — Slim down to orchestration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Move task-specific configuration classes still living in `iped-engine` to the owning `iped-tasks-*` module, following the pattern already started with the forensics tasks split.

## Problem

Several task-specific config classes remain in `iped-engine` rather than their owning task module. Typed config access via `ITypedConfigAccess` already removes the hard dependency, but the physical class moves are deferred pending coordinated changes across `iped-tasks-*` modules.

## Acceptance criteria

- [x] Provide `ITypedConfigAccess` so tasks/parsers don't need to import engine config internals directly.
- [ ] Move remaining task-specific config classes to their owning `iped-tasks-*` module.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `in_progress`.
