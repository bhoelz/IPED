# ISSUE-172: Serve geo features as GeoJSON from iped-webapi for browser map view

- Status: done
- Roadmap: [iped-geo-ROADMAP.md](../roadmaps/iped-geo-ROADMAP.md)
- Roadmap section: Phase 2 — Web map experience (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add GeoJSON endpoints to `iped-webapi` so the browser UI can render a map view with a standard web map library (Leaflet/MapLibre) instead of the embedded WebKit map.

## Problem

There was no API surface for serving case geolocation data as GeoJSON, which is required before any browser-based map rendering can replace the embedded WebKit map view.

## Acceptance criteria

- [x] `GET /v2/sources/{sourceId}/geo` returns a GeoJSON FeatureCollection for all geolocated items (via `SearchService` with an `ExtraProperties.LOCATIONS:*` query; default limit 50 000; `truncated: true` when larger; `total` reflects the full count).
- [x] `GET /v2/sources/{sourceId}/items/{id}/geo` returns a single-item FeatureCollection (0 or more features, e.g. for GPX tracks with multiple LOCATIONS values).
- [x] GeoJSON coordinate order is [longitude, latitude, altitude?] per RFC 7946.
- [x] JSON built with a `StringBuilder` to avoid adding a JSON serializer dependency.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-geo-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
