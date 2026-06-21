# ISSUE-134: Finish de-singletoning remaining static state under concurrent-case load

- Status: planned
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 3 — Multi-case and distributed maturity
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Finish de-singletoning per `ARCHITECTURE.md` (CaseContext/ThreadLocal model) for any remaining static state discovered under concurrent-case load, across the whole engine-split effort.

## Problem

Individual submodules (`iped-engine`, `iped-engine-core`) have already fixed several instances of static mutable state unsafe under concurrent multi-case processing (see ISSUE-086, ISSUE-103), but this parent roadmap item tracks the cross-cutting sweep to confirm no further instances remain anywhere in the split.

## Acceptance criteria

- [ ] Cross-module audit for remaining static/singleton state outside `iped-engine` and `iped-engine-core` (already covered by ISSUE-086/ISSUE-103).
- [ ] Confirm no regressions reintroduce static state during ongoing module split work.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `planned`.
