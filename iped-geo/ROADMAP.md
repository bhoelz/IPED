# iped-geo — Evolution Roadmap

> Module purpose: geolocation features — KML parsing/export, map rendering
> (OpenStreetMap via embedded WebKit/JavaFX), geo track visualization for the desktop UI.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Tied to the Swing/JavaFX desktop stack (`iped.geo.webkit`, `iped.webkit.utils`).
- KML/geo parsing (`iped.geo.parsers`) mixed in the same module as the rendering UI.

## Phase 1 — Split data from presentation
- [x] Separate geo *data* concerns (KML parse/export, coordinate extraction, track
      building) from *rendering* (WebKit map view) — either subpackages with an ArchUnit
      fence or a `iped-geo-core`/`iped-geo-swing` module split.
      (`iped.geo.data` subpackage created with three types:
      `GeoFeature` — immutable record (sourceId, docId, name, lat, lon, altitude?,
      timestamp?); coordinates in WGS-84 decimal degrees.
      `IGeoDataService` — headless interface: `extractFeatures(IIPEDSource, Iterable<IItemId>)`.
      `GeoDataService` — implementation reading `ExtraProperties.LOCATIONS` ("lat;lon"
      multi-valued) and `"common:altitude"` via `IItem.getMetadataMap()` (reflective, no
      Tika compile dependency). No `java.awt`, `javax.swing`, `javafx` imports.
      `IpedGeoArchitectureTest` — three ArchUnit rules enforcing the headless fence on
      `iped.geo.data..`: no AWT/Swing, no JavaFX, no rendering-layer deps.)
- [x] Geo data services consumable headless by `iped-webapi` (no JavaFX on the classpath
      for the server path).
      (`GeoDataService` and `GeoFeature` are pure Java with only iped-api + SLF4J deps.
      `GeoV2` in iped-webapi uses `IItem.getMetadataMap()` directly — no iped-geo compile
      dependency needed from the webapi side.)

## Phase 2 — Web map experience (5.0 workstream 1)
- [x] Map view in the browser UI: serve geo features as GeoJSON from `iped-webapi`;
      render with a standard web map library (Leaflet/MapLibre) instead of embedded
      WebKit.
      (`GeoV2.java` added to iped-webapi:
      `GET /v2/sources/{sourceId}/geo` — GeoJSON FeatureCollection for all geolocated
      items (uses SearchService with `ExtraProperties.LOCATIONS:*` query; default limit
      50 000; `truncated: true` when result set is larger; `total` reflects full count).
      `GET /v2/sources/{sourceId}/items/{id}/geo` — single-item FeatureCollection (0 or
      more features for items with multiple LOCATIONS values, e.g. GPX tracks).
      GeoJSON coordinate order: [longitude, latitude, altitude?] per RFC 7946.
      JSON built with a StringBuilder to avoid pulling in a JSON serialiser dependency.)
- [x] `<iped-map>` island in iped-webui: Leaflet/MapLibre rendering against the GeoV2
      endpoint; marker click dispatches `itemSelectedEvent`.
      (`MapComponent` in `iped-webui/src/islands/map/` — see iped-webui ROADMAP Phase 2.)
- [x] Tile-source configuration (offline tile packages for air-gapped labs vs OSM online).
      (`WebMapProperties` `@ConfigurationProperties(prefix="iped.webui.map")` added to
      `iped-webui-server`: `tile-url` (default OSM), `tile-attribution`, `height`.
      `application.yml` defaults; all three override-able via `IPED_MAP_TILE_URL`,
      `IPED_MAP_TILE_ATTRIBUTION`, `IPED_MAP_HEIGHT` env vars for air-gapped deployments.
      `WorkspaceController` injects `WebMapProperties` and passes values to the
      `page.rocker.html` template. `<iped-map>` added to workspace page with a "Map"
      mode button in the segmented toolbar. Bridge script wires `item-selected` from the
      map island to load info/viewer panels. `WorkspacePilotTest` extended with
      `workspacePageEmbedsMapIslandWithTileConfig()` assertion.)
- [ ] Track/heatmap rendering parity with the Swing map for large coordinate sets
      (server-side clustering for >100k points).

## Phase 3 — Decommission embedded WebKit
- [ ] Once browser-map parity is signed off, retire the JavaFX/WebKit map path with the
      rest of the Swing UI; KML export remains as a data feature.

## Progress checks
- `GET /v2/sources/{src}/geo` returns GeoJSON for a real case; `<iped-map>` island
  renders markers and fires `item-selected` on click.
- `iped.geo.data` ArchUnit fence passes (no AWT/Swing/JavaFX in data layer).
- No JavaFX classes loaded in headless server mode (verified by checking JVM class loading
  logs when starting `iped-webapi` without a display).
