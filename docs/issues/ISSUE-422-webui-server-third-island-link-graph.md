# ISSUE-422: Third island: link-graph view wired into the workspace

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 2 — Island growth
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`<iped-graph case-id api-base>` is registered in `iped-webui`'s `main.ts`; the SSR workspace page includes it at `<iped-graph data-view="graph" hidden>` alongside a "Links" mode button. No SSR fragment is needed since the island owns its own rendering via the API base.

## Problem

The workspace needed a graph/relationships view, sourced from the `GraphComponent` built in `iped-webui`, without requiring a parallel server-rendered fragment.

## Acceptance criteria

- [x] `<iped-graph>` registered as a custom element and embedded in the workspace page.
- [x] "Links" mode button toggles the graph view via `ipedSetMode('graph')`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
