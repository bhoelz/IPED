# ISSUE-136: Engine remains authoritative for case/search semantics via iped-webapi contracts

- Status: planned
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0 alignment
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Ensure the engine remains the sole source of truth for case/search semantics, with web/MCP layers consuming it only through `iped-webapi` contracts.

## Problem

As `iped-webapi` and `iped-mcp` mature as consumers of the engine, there's a risk of these layers reimplementing or bypassing case/search semantics directly instead of going exclusively through `iped-webapi` contracts, fragmenting the source of truth.

## Acceptance criteria

- [ ] Audit `iped-mcp` and any other web/MCP consumer for direct engine access bypassing `iped-webapi`.
- [ ] Document the contract boundary: case/search semantics are owned by the engine, exposed only via `iped-webapi`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `planned`.
