# ISSUE-174: Tile-source configuration for offline/air-gapped map deployments

- Status: done
- Roadmap: [iped-geo-ROADMAP.md](../roadmaps/iped-geo-ROADMAP.md)
- Roadmap section: Phase 2 — Web map experience (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add configurable tile-source settings for the web map so labs running air-gapped can point at offline tile packages instead of the public OpenStreetMap servers.

## Problem

A hardcoded OSM tile source would break the map view for air-gapped forensic labs that cannot reach the public internet; tile source needed to be configurable per deployment.

## Acceptance criteria

- [x] `WebMapProperties` `@ConfigurationProperties(prefix="iped.webui.map")` added to `iped-webui-server`: `tile-url` (default OSM), `tile-attribution`, `height`.
- [x] `application.yml` defaults provided; all three overridable via `IPED_MAP_TILE_URL`, `IPED_MAP_TILE_ATTRIBUTION`, `IPED_MAP_HEIGHT` env vars.
- [x] `WorkspaceController` injects `WebMapProperties` and passes values to `page.rocker.html`.
- [x] `<iped-map>` added to the workspace page with a "Map" mode button in the segmented toolbar.
- [x] Bridge script wires `item-selected` from the map island to load info/viewer panels.
- [x] `WorkspacePilotTest` extended with `workspacePageEmbedsMapIslandWithTileConfig()` assertion.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-geo-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
