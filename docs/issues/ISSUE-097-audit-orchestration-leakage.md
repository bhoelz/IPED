# ISSUE-097: Audit for orchestration logic leaked from iped-engine

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary enforcement
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Audit `iped-engine-core` for any orchestration logic (Manager/Worker/Statistics) that leaked down from `iped-engine`.

## Problem

Orchestration concerns belong exclusively in `iped-engine`; any Manager/Worker/Statistics references in `iped-engine-core` would violate the module's "no orchestration code" charter.

## Acceptance criteria

- [x] Audit complete: no Manager/Worker/Statistics references found in `iped-engine-core`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
