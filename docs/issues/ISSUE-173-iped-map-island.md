# ISSUE-173: iped-map island rendering against the GeoV2 endpoint

- Status: done
- Roadmap: [iped-geo-ROADMAP.md](../roadmaps/iped-geo-ROADMAP.md)
- Roadmap section: Phase 2 — Web map experience (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a `<iped-map>` island in `iped-webui` that renders against the `GeoV2` endpoint using Leaflet/MapLibre, with marker clicks dispatching an `itemSelectedEvent` so the rest of the workspace UI can respond.

## Problem

The browser UI had no map-rendering component; this is the client-side counterpart to the GeoJSON endpoints needed to fully replace the embedded WebKit map.

## Acceptance criteria

- [x] `MapComponent` added in `iped-webui/src/islands/map/`.
- [x] Renders Leaflet/MapLibre map against the `GeoV2` endpoint.
- [x] Marker click dispatches `itemSelectedEvent`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-geo-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
