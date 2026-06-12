# iped-geo — Evolution Roadmap

> Module purpose: geolocation features — KML parsing/export, map rendering
> (OpenStreetMap via embedded WebKit/JavaFX), geo track visualization for the desktop UI.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Tied to the Swing/JavaFX desktop stack (`iped.geo.webkit`, `iped.webkit.utils`).
- KML/geo parsing (`iped.geo.parsers`) mixed in the same module as the rendering UI.

## Phase 1 — Split data from presentation
- [ ] Separate geo *data* concerns (KML parse/export, coordinate extraction, track
      building) from *rendering* (WebKit map view) — either subpackages with an ArchUnit
      fence or a `iped-geo-core`/`iped-geo-swing` module split.
- [ ] Geo data services consumable headless by `iped-webapi` (no JavaFX on the classpath
      for the server path).

## Phase 2 — Web map experience (5.0 workstream 1)
- [ ] Map view in the browser UI: serve geo features as GeoJSON from `iped-webapi`;
      render with a standard web map library (Leaflet/MapLibre) instead of embedded
      WebKit.
- [ ] Tile-source configuration (offline tile packages for air-gapped labs vs OSM
      online) in TOML config.
- [ ] Track/heatmap rendering parity with the Swing map for large coordinate sets
      (server-side clustering for >100k points).

## Phase 3 — Decommission embedded WebKit
- [ ] Once browser-map parity is signed off, retire the JavaFX/WebKit map path with the
      rest of the Swing UI; KML export remains as a data feature.

## Progress checks
- `GET` geo endpoints return GeoJSON for a real case; browser map renders tracks.
- No JavaFX classes loaded in headless server mode.
