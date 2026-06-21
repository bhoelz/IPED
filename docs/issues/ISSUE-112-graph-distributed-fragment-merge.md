# ISSUE-112: Deterministic merge of per-segment graph fragments

- Status: planned
- Roadmap: [iped-engine-graph-ROADMAP.md](../roadmaps/iped-engine-graph-ROADMAP.md)
- Roadmap section: Phase 2 — Storage strategy
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Graph fragments produced by distributed agents on a per-segment basis must merge deterministically into one consistent graph, applying the same constraint that already exists for the Lucene index merge in distributed processing.

## Problem

Distributed processing produces graph data per segment/agent; there is currently no defined deterministic merge strategy to combine these fragments into a single consistent case graph.

## Acceptance criteria

- [ ] A merge strategy for per-segment graph fragments is implemented.
- [ ] Merged output is deterministic and matches the same case processed monolithically.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-graph-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
