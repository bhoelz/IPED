# ISSUE-388: GeoJSON endpoint for geo-tagged items

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 7 — Geo and format conversion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Expose geo-tagged evidence as GeoJSON so the browser UI can render maps of item locations, including multi-point items like GPX tracks or KML multi-waypoint files.

## Problem

There was no API surface for exposing items' geolocation metadata in a map-renderable format.

## Acceptance criteria

- [x] `GeoV2.java` — `GET /v2/sources/{src}/geo` returns a GeoJSON FeatureCollection for items with `ExtraProperties.LOCATIONS` (`"common:geo:locations"`) metadata.
- [x] Each `"lat;lon"` value becomes a GeoJSON Point Feature; items with multiple location values contribute multiple features.
- [x] `total` and `truncated` fields handle large datasets; default limit 50,000, max 500,000.
- [x] `GET /v2/sources/{src}/items/{id}/geo` returns features for a single item; coordinate order is [longitude, latitude, altitude?].
- [x] `GeoV2Test` — 8 unit tests via reflection covering output format, coordinate order, altitude injection, JSON escaping, and truncation metadata.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
