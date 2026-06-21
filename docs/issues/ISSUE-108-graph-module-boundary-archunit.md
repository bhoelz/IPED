# ISSUE-108: Enforce iped-engine-graph module boundary via ArchUnit and SPI hooks

- Status: planned
- Roadmap: [iped-engine-graph-ROADMAP.md](../roadmaps/iped-engine-graph-ROADMAP.md)
- Roadmap section: Phase 1 — Module boundary
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The `iped-engine-graph` module should depend only on `iped-engine-core` and `iped-api`, verified and enforced with an ArchUnit rule. Graph generation hooks must reach the engine through task/listener SPIs rather than direct imports, keeping the module properly decoupled.

## Problem

Without an enforced rule, the graph module risks accumulating direct dependencies on engine internals beyond `iped-engine-core`/`iped-api`, undermining the modular boundary established when it was extracted.

## Acceptance criteria

- [ ] ArchUnit rule verifies `iped-engine-graph` depends only on `iped-engine-core` + `iped-api`.
- [ ] Graph generation hooks integrate via task/listener SPIs, not direct imports of engine internals.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-graph-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
