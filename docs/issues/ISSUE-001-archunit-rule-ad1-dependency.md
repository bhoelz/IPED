# ISSUE-001: ArchUnit rule enforcing the ad1 dependency constraint

- Status: planned
- Roadmap: [iped-ad1-ROADMAP.md](../roadmaps/iped-ad1-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The `iped-ad1` module is only allowed to depend on `iped-engine-core` and `iped-api`. An automated ArchUnit rule is needed to enforce this constraint so it cannot silently regress as the module evolves.

## Problem

There is currently no automated check that `iped-ad1` stays within its allowed dependency set (`iped-engine-core` + `iped-api`). Without an enforced rule, accidental coupling to other modules could be introduced and go unnoticed.

## Acceptance criteria

- [ ] An ArchUnit test exists that fails the build if `iped-ad1` depends on anything outside `iped-engine-core` and `iped-api`.
- [ ] The rule runs as part of the module's standard test suite.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ad1-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
