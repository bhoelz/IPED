# ISSUE-325: ArchUnit rule enforcing iped-ufed dependency constraint

- Status: planned
- Roadmap: [iped-ufed-ROADMAP.md](../roadmaps/iped-ufed-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`iped-ufed` should depend only on `iped-engine-core` and `iped-api`, with that constraint enforced via an ArchUnit rule.

## Problem

There is no automated check today preventing `iped-ufed` from acquiring dependencies outside its allowed set.

## Acceptance criteria

- [ ] ArchUnit test fails the build if `iped-ufed` depends on anything outside `iped-engine-core` + `iped-api`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ufed-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
