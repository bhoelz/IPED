# ISSUE-018: Distributed-processing support for per-segment additional-index output

- Status: planned
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 3 — Operations
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

For the Kafka-based distributed processing pipeline, additional-index output produced per processing segment needs either a merge strategy or a late-indexing strategy, decided on a per-connector basis.

## Problem

Distributed agents process evidence in segments; there is currently no defined strategy for how each connector's per-segment additional-index output gets combined into a coherent whole.

## Acceptance criteria

- [ ] Each connector type has a documented merge-or-late-indexing strategy for distributed output.
- [ ] Strategy is implemented for at least one connector as a reference.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
