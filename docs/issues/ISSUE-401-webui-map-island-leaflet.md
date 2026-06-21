# ISSUE-401: Map island — Leaflet island consuming GeoV2 GeoJSON endpoint

- Status: done
- Roadmap: [iped-webui-ROADMAP.md](../roadmaps/iped-webui-ROADMAP.md)
- Roadmap section: Phase 2 — Island buildout
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`MapComponent` (`<iped-map>`) extends `IslandBase`, fetches `GET /v2/sources/{src}/geo` on `source-id` change, renders Leaflet markers with auto-fit bounds, supports a configurable tile URL/attribution for air-gapped deployments, and dispatches both `map-marker-selected` and the standard `item-selected` event on marker click.

## Problem

Geo data needed a map-based island consuming the GeoV2 endpoint, with support for offline/air-gapped tile servers and cross-island item selection.

## Acceptance criteria

- [x] `<iped-map>` fetches `GET /v2/sources/{src}/geo` and renders markers via Leaflet.
- [x] `tile-url`/`tile-attribution` inputs support air-gapped deployments.
- [x] Marker click dispatches `map-marker-selected` and `item-selected`.
- [x] Truncation notice shown when `truncated: true`; error state + `island-error` on HTTP failure.
- [x] `leaflet` added to dependencies, `@types/leaflet` to devDependencies; 8 component tests with Leaflet stubbed.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
