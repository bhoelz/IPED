# ISSUE-119: Hash-lookup SPI for task consumption

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 1 — Module boundary
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Tasks should consume hash lookups through an interface rather than a concrete implementation, enabling alternate backends and clean unit testing without requiring a real hash database file.

## Problem

Tasks that need hash lookups are likely coupled to a concrete hash DB implementation today, making them harder to unit test and harder to swap backends for.

## Acceptance criteria

- [ ] A hash-lookup SPI/interface is defined and used by consuming tasks.
- [ ] Tasks can be unit tested against a fake/mock implementation without a real hash DB file.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
