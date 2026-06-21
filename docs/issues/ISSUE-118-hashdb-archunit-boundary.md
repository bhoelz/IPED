# ISSUE-118: ArchUnit rule for iped-engine-hashdb dependency boundary

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 1 — Module boundary
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`iped-engine-hashdb` should depend only on `iped-engine-core` and `iped-api`, with that constraint enforced by an ArchUnit rule to prevent accidental coupling.

## Problem

No automated check currently exists to keep `iped-engine-hashdb` within its allowed dependency set.

## Acceptance criteria

- [ ] ArchUnit test fails the build if `iped-engine-hashdb` depends on anything outside `iped-engine-core` + `iped-api`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
