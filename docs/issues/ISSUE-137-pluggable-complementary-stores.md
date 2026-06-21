# ISSUE-137: Pluggable complementary stores integrate via iped-additional-index connectors

- Status: planned
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0 alignment
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Ensure pluggable complementary stores (graph/vector/time-series) integrate via `iped-additional-index` connector contracts, never via direct engine core changes.

## Problem

Without an enforced connector contract, future complementary store integrations (graph, vector, time-series) risk being wired directly into engine core, re-coupling the engine to optional capabilities it should not depend on.

## Acceptance criteria

- [ ] Define the `iped-additional-index` connector contract for complementary stores.
- [ ] Confirm graph store integration (`iped-engine-graph`) goes through the connector contract, not direct engine core changes.
- [ ] Document the pattern for future vector/time-series store integrations.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `planned`.
