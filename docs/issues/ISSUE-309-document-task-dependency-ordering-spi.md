# ISSUE-309: Document task dependency/ordering declaration in SPI Javadoc

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 2 — SPI maturity
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Task dependency and ordering declaration is now documented in the SPI Javadoc,
covering the `TaskDependency` factory methods (`requires`, `before`, `after`).

## Problem

Third-party task implementors had no documented way to declare ordering
relationships against other tasks.

## Acceptance criteria

- [x] `TaskDependency` factory methods (`requires`, `before`, `after`) documented in
      SPI Javadoc.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
