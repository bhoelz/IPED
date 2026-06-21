# ISSUE-015: Graph store coordination with iped-engine-graph

- Status: planned
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 2 — First-class connectors
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The additional-index module needs to coordinate with `iped-engine-graph` so there is one relationship model shared between them, with the actual storage backend selectable via configuration through the connector contract.

## Problem

Without coordination, `iped-engine-graph` and the additional-index connector framework could each define their own relationship model, causing duplication or inconsistency.

## Acceptance criteria

- [ ] A single relationship model is shared between `iped-additional-index` and `iped-engine-graph`.
- [ ] The graph storage backend is selectable via configuration rather than hardcoded.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
