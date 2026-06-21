# ISSUE-170: Split geo data concerns from rendering

- Status: done
- Roadmap: [iped-geo-ROADMAP.md](../roadmaps/iped-geo-ROADMAP.md)
- Roadmap section: Phase 1 — Split data from presentation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Separate geo *data* concerns (KML parse/export, coordinate extraction, track building) from *rendering* (the embedded WebKit map view), either via subpackages with an ArchUnit fence or a full `iped-geo-core`/`iped-geo-swing` module split, so headless consumers don't need the rendering stack.

## Problem

`iped.geo.parsers` (data) and `iped.geo.webkit` (rendering) were mixed in the same module, making it impossible to consume geo data headlessly without pulling in the WebKit/JavaFX rendering stack.

## Acceptance criteria

- [x] `iped.geo.data` subpackage created.
- [x] `GeoFeature` immutable record added (sourceId, docId, name, lat, lon, altitude?, timestamp?; WGS-84 decimal degrees).
- [x] `IGeoDataService` headless interface added (`extractFeatures(IIPEDSource, Iterable<IItemId>)`).
- [x] `GeoDataService` implementation reads `ExtraProperties.LOCATIONS` and `"common:altitude"` via `IItem.getMetadataMap()` reflectively, with no Tika compile dependency.
- [x] `IpedGeoArchitectureTest` added with ArchUnit rules forbidding AWT/Swing, JavaFX, and rendering-layer deps in `iped.geo.data..`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-geo-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
