# ISSUE-016: Rebuild tooling for additional indexes from the case index

- Status: planned
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 3 — Operations
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Operators need tooling to regenerate any additional index (vector, time-series, graph) directly from the authoritative case index, without having to reprocess the original evidence.

## Problem

There is currently no way to rebuild an additional index independently of full evidence reprocessing, which would be costly and slow for recovery or migration scenarios.

## Acceptance criteria

- [ ] Tooling exists to regenerate a given additional index from the case index alone.
- [ ] Reprocessing of original evidence is not required for a rebuild.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
