# ISSUE-225: Make parser execution compatible with the distributed work-unit model

- Status: planned
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

For the `iped-distributed` work-unit model, parsers must either be stateless
per-item or explicitly declare their state requirements, mirroring the
state-model classification work already done for tasks (see
`iped-tasks-ROADMAP.md` Phase 3).

## Problem

Today parsers have no formal state-model declaration, so the distributed
coordinator cannot safely determine which parsers can run as independent
distributed work units without risking shared-state corruption.

## Acceptance criteria

- [ ] Audit parser classes for static/shared state, mirroring the task
      classification matrix approach.
- [ ] Define a way for parsers to declare statelessness or state requirements,
      consumable by the `iped-distributed` work-unit model.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
