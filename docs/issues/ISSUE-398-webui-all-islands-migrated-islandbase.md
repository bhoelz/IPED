# ISSUE-398: All remaining islands migrated to IslandBase with typed events

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 2 — Island buildout
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`GalleryComponent`, `GraphComponent`, `HexViewerComponent`, and `TimelineComponent` all extend `IslandBase`; their `@Output` declarations were removed in favor of typed `CustomEvent` dispatch via `this.dispatch()`.

## Problem

Several islands still used Angular's `EventEmitter`/`@Output` pattern, which doesn't cross custom-element boundaries cleanly; they needed migration to the shared `IslandBase` contract.

## Acceptance criteria

- [x] Gallery, Graph, HexViewer, and Timeline all extend `IslandBase`.
- [x] All `@Output` declarations removed from these four components.
- [x] Each component dispatches its events via `this.dispatch()` using the typed factories.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
