# ISSUE-131: Add ArchUnit rules encoding dependency constraints in each submodule

- Status: planned
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 1 — Complete the split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add ArchUnit rules in each submodule encoding the architectural dependency constraints (datasource modules depend only on `iped-engine-core`; `iped-engine-core` has no sibling dependency; `iped-distributed` never pulls Kafka types into engine code) so regressions fail the build instead of being caught in review.

## Problem

Several individual submodule roadmaps (`iped-engine-core`, `iped-engine`) already have their own local ArchUnit boundary tests, but there is no cross-cutting confirmation that every submodule in the split has an enforced rule matching the target architecture described in this parent roadmap.

## Acceptance criteria

- [ ] Confirm/add ArchUnit rule in `iped-sleuthkit`/`iped-ufed`/`iped-ad1` restricting dependencies to `iped-engine-core` only.
- [ ] Confirm/add ArchUnit rule in `iped-engine-core` forbidding sibling dependencies (tracked individually as ISSUE-096).
- [ ] Confirm/add ArchUnit rule preventing Kafka types in `iped-engine` (tracked individually as ISSUE-084).
- [ ] Wire all submodule ArchUnit checks into the reactor build so a regression fails CI.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `planned`.
