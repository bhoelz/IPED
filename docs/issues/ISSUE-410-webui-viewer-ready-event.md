# ISSUE-410: viewerReadyEvent added to events.ts

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 5 — Viewer island and event contract extension
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`viewerReadyEvent` was added to `events.ts` along with the `ViewerReadyDetail` interface and `ViewerType` union, completing the typed event contract needed by the viewer-host island.

## Problem

The viewer-host island needed a typed event to announce when it has finished loading and which renderer/MIME type it resolved to.

## Acceptance criteria

- [x] `viewerReadyEvent` factory and `ViewerReadyDetail`/`ViewerType` types added to `events.ts`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
