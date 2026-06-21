# ISSUE-110: Evaluate embedded graph DB choice for 5.0

- Status: planned
- Roadmap: [iped-engine-graph-ROADMAP.md](../roadmaps/iped-engine-graph-ROADMAP.md)
- Roadmap section: Phase 2 — Storage strategy
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

For the 5.0 release, the team needs to decide whether to keep the current embedded graph store or move to the pluggable graph-store connector planned under root roadmap workstream 5 (shared with `iped-additional-index`).

## Problem

The current embedded graph DB choice has not been validated against the long-term 5.0 pluggable-connector direction, risking a costly storage migration later if not planned now.

## Acceptance criteria

- [ ] A decision is documented: keep current embedded store vs adopt the pluggable graph-store connector.
- [ ] Decision is cross-referenced with the `iped-additional-index` connector workstream.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-graph-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
