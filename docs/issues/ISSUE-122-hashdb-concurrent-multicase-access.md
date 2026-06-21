# ISSUE-122: Verify concurrent multi-case access to a shared hash DB

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 2 — Performance and formats
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The read path for a shared hash DB needs verification that it is lock-free or properly shared — one mapped database serving all `CaseContexts`, rather than a separate copy loaded per case, which would waste memory and I/O.

## Problem

It's unclear whether the current implementation safely shares one hash DB mapping across concurrent cases or duplicates it per case, which has performance and correctness implications.

## Acceptance criteria

- [ ] Verified (with a test) that one mapped hash DB serves multiple concurrent `CaseContexts`.
- [ ] No contention errors occur under concurrent multi-case access.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
