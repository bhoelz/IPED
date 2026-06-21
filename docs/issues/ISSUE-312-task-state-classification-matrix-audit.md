# ISSUE-312: Audit and record the task state-model classification matrix

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A code audit classified each major task by its actual state model
(`STATELESS`/`CASE_SCOPED`/`GLOBAL`) and recorded the key shared-state issue found
for each, producing the classification matrix now kept in the roadmap's Progress
checks section.

## Problem

Before any case-scoping fixes could be planned (ISSUE-313 through ISSUE-319), the
team needed a ground-truth inventory of which tasks held unsafe global/static
state and why.

## Acceptance criteria

- [x] Classification matrix produced for `HashTask`, `ExportFileTask`,
      `ImageThumbTask`, `VideoThumbTask`, `CarverTask`, `KnownMetCarveTask`,
      `LedCarveTask`, `HtmlReportTask`, `GraphTask`.
- [x] Each entry records the state model and the key state-sharing issue found.
- [x] Matrix recorded in `iped-tasks-ROADMAP.md`'s Progress checks section.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
