# ISSUE-311: Add TaskStateModel enum to iped-tasks.spi

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A `TaskStateModel` enum (`STATELESS`, `CASE_SCOPED`, `GLOBAL`) was added to
`iped-tasks.spi`, with `TaskProvider.stateModel()` defaulting to `GLOBAL` and
providers overriding to declare a safer model.

## Problem

There was no formal way for a task implementation to declare its state-sharing
model, which is a prerequisite for the distributed/multi-case work-unit model to
reason about which tasks are safe to parallelize across cases or nodes.

## Acceptance criteria

- [x] `TaskStateModel` enum added to `iped-tasks.spi` with `STATELESS`,
      `CASE_SCOPED`, `GLOBAL` values.
- [x] `TaskProvider.stateModel()` default method added, defaulting to `GLOBAL`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
