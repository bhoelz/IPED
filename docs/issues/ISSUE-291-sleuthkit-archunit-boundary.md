# ISSUE-291: ArchUnit rule enforcing iped-sleuthkit dependency constraint

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`iped-sleuthkit` should depend only on `iped-engine-core` and `iped-api`, with no dependency on `iped-engine` or sibling datasource modules, enforced via an ArchUnit rule.

## Problem

There is no automated rule today preventing `iped-sleuthkit` from acquiring forbidden dependencies on `iped-engine` or its sibling datasource modules.

## Acceptance criteria

- [ ] ArchUnit test fails the build if `iped-sleuthkit` depends on `iped-engine` or any sibling datasource module.
- [ ] Only `iped-engine-core` and `iped-api` are allowed as dependencies.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
