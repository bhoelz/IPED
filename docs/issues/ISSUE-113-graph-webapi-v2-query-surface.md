# ISSUE-113: Expose graph queries through iped-webapi v2

- Status: planned
- Roadmap: [iped-engine-graph-ROADMAP.md](../roadmaps/iped-engine-graph-ROADMAP.md)
- Roadmap section: Phase 3 — Analysis features
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Graph queries — link expansion, shortest path, neighborhood — should be exposed through `iped-webapi` v2 so the browser-based UI's Links view can work without depending on the Swing desktop client.

## Problem

The Links view currently depends on Swing-based access to graph data, blocking a browser-first UI experience for relationship analysis.

## Acceptance criteria

- [ ] `iped-webapi` v2 exposes link expansion, shortest path, and neighborhood queries.
- [ ] Browser UI Links view can be implemented using only the webapi endpoints (no Swing dependency).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-graph-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
