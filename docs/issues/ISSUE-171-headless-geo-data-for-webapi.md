# ISSUE-171: Geo data services consumable headless by iped-webapi

- Status: done
- Roadmap: [iped-geo-ROADMAP.md](../roadmaps/iped-geo-ROADMAP.md)
- Roadmap section: Phase 1 — Split data from presentation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Make geo data services consumable headlessly by `iped-webapi`, so the server path has no JavaFX on its classpath, enabling the browser-based map experience without the desktop rendering dependency.

## Problem

`iped-webapi` needed access to geolocation data without compiling against `iped-geo`'s JavaFX-dependent rendering classes, which would be unacceptable for a headless server deployment.

## Acceptance criteria

- [x] `GeoDataService` and `GeoFeature` are pure Java with only `iped-api` + SLF4J dependencies.
- [x] `GeoV2` in `iped-webapi` uses `IItem.getMetadataMap()` directly, with no `iped-geo` compile dependency from the webapi side.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-geo-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
