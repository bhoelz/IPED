# iped-geo — Evolution Roadmap

> Module purpose: geolocation features — KML parsing/export, map rendering
> (OpenStreetMap via embedded WebKit/JavaFX), geo track visualization for the desktop UI.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Tied to the Swing/JavaFX desktop stack (`iped.geo.webkit`, `iped.webkit.utils`).
- KML/geo parsing (`iped.geo.parsers`) mixed in the same module as the rendering UI.

## Phase 1 — Split data from presentation — `done`
- [ISSUE-170](../issues/ISSUE-170-split-geo-data-from-rendering.md) — Split geo data concerns from rendering — `done`
- [ISSUE-171](../issues/ISSUE-171-headless-geo-data-for-webapi.md) — Geo data services consumable headless by iped-webapi — `done`

## Phase 2 — Web map experience (5.0 workstream 1) — `in_progress`
- [ISSUE-172](../issues/ISSUE-172-geojson-map-endpoints.md) — Serve geo features as GeoJSON from iped-webapi for browser map view — `done`
- [ISSUE-173](../issues/ISSUE-173-iped-map-island.md) — iped-map island rendering against the GeoV2 endpoint — `done`
- [ISSUE-174](../issues/ISSUE-174-tile-source-configuration.md) — Tile-source configuration for offline/air-gapped map deployments — `done`
- [ISSUE-175](../issues/ISSUE-175-track-heatmap-rendering-parity.md) — Track/heatmap rendering parity for large coordinate sets — `planned`

## Phase 3 — Decommission embedded WebKit — `planned`
- [ISSUE-176](../issues/ISSUE-176-decommission-embedded-webkit.md) — Decommission the embedded JavaFX/WebKit map path — `planned`

## Progress checks
- `GET /v2/sources/{src}/geo` returns GeoJSON for a real case; `<iped-map>` island
  renders markers and fires `item-selected` on click.
- `iped.geo.data` ArchUnit fence passes (no AWT/Swing/JavaFX in data layer).
- No JavaFX classes loaded in headless server mode (verified by checking JVM class loading
  logs when starting `iped-webapi` without a display).
