# ISSUE-111: Define a stable export format for graph data

- Status: planned
- Roadmap: [iped-engine-graph-ROADMAP.md](../roadmaps/iped-engine-graph-ROADMAP.md)
- Roadmap section: Phase 2 — Storage strategy
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A stable export format for graph data is needed so the underlying storage backend can be changed in the future without requiring full case reprocessing.

## Problem

Graph data is currently tied to its storage backend's native representation, which would force a full reprocess if the backend changes.

## Acceptance criteria

- [ ] A backend-independent export format for graph nodes/edges is defined.
- [ ] Graph data can be migrated to a new backend via the export format without reprocessing the case.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-graph-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
