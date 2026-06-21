# ISSUE-319: Add an explicit end-of-case finalization stage for distributed coordination

- Status: blocked
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Tasks with end-of-case phases (graph generation, report, storage-index) need an
explicit "finalization" stage that the distributed coordinator can invoke once
after all segments merge. Investigation found this isn't yet buildable: there is no
cross-node merge machinery, and the relevant tasks haven't migrated to the
`TaskProvider` SPI yet.

## Problem

`CaseLifecycleManager.completeCase()` (`iped-distributed`) only flips a status flag
and persists — nothing combines graph CSVs, report HTML, or carved-item lists
produced by separate nodes. `GraphTask`/`HTMLReportTask`/`CarverTask` also still
extend the legacy `AbstractTask`, not the newer `TaskProvider` SPI, so a
`finalizeCase()` hook added to that SPI today would have no caller. This item is
explicitly blocked pending prerequisite work.

## Acceptance criteria

- [ ] Build the actual segment-merge step in `iped-distributed` (what gets merged
      and how, per task family).
- [ ] Migrate `GraphTask`/`HTMLReportTask`/`CarverTask` onto the `TaskProvider` SPI.
- [ ] Add the finalization hook to `TaskProvider` that the merge step invokes, only
      once the prior two steps are done.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `blocked` because the item's own text explicitly says it is investigated but not yet buildable pending external prerequisites.
