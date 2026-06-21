# ISSUE-011: Define the pluggable additional-store connector SPI

- Status: planned
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 1 — Connector contract
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

As part of the root roadmap's 5.0 "additional data stores" workstream, a pluggable connector SPI is needed so graph, vector, and time-series stores can be plugged into the additional-index module uniformly. The SPI must define lifecycle (open/index/commit/close), an item-ID traceability requirement, and a per-connector consistency policy declaration.

## Problem

There is currently no common contract for additional-store connectors, which would force each new store type (vector, time-series, graph) to invent its own integration approach.

## Acceptance criteria

- [ ] SPI defines connector lifecycle: open/index/commit/close.
- [ ] SPI requires connectors to declare item-ID traceability back to original evidence.
- [ ] SPI requires connectors to declare their consistency policy.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
