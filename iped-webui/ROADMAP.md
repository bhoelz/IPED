# iped-webui — Evolution Roadmap

> Module purpose: Angular 21 frontend workspace — originally the SPA shell for the new
> web UI (EPIC-WEB-01), now repositioned as the **island source** for the Spring Boot
> SSR composition root (`iped-webui-server`). See `REFACTORING_PLAN.md` at the repo root.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Angular Elements `islands` build target (zoneless, signals, hashed bundle).
- [x] First island: results grid, consumed by the SSR workspace page.
- Legacy SPA (`WorkspacePage` + domains structure) still present; OpenAPI client
  generation (`npm run generate:api`) wired to the `iped-webapi` spec.

## Phase 1 — Island library consolidation
- [x] Restructure `src/` around islands as the primary artifact (`src/islands/*`);
      mark the SPA shell as legacy (no new features).
      (`src/islands/` is the canonical location for all island components. The SPA shell
      (`WorkspacePage`, domain modules) remains but is frozen — no new features added.)
- [x] Shared island infrastructure: typed attribute/event contracts, generated API
      client usage inside islands, design tokens via CSS variables only (no SCSS theme
      coupling to the SSR shell).
      (`src/islands/shared/` created with three modules:
       - `api.ts`: `API_BASE_URL` DI token (default `/api`, overridable per-island
         via `api-base` attribute).
       - `events.ts`: typed `CustomEvent` factory functions — `itemSelectedEvent`,
         `selectionChangedEvent`, `resultsLoadedEvent`, `islandErrorEvent` — with
         detail interfaces for each. Events are `bubbles: true, composed: true`.
       - `island-base.ts`: abstract `IslandBase` directive with `hostEl`, `apiBase`,
         and `dispatch<T>()` helper. `ResultsGridComponent` migrated to extend it.)
- [x] Island build pipeline: per-island bundles with independent hashes (today: single
      bundle) so adding islands doesn't bust all caches.
      (`main.ts` changed to dynamic `import()` for each island component; Vite/esbuild
      emits one hashed chunk per island automatically — updating `results-grid` no longer
      invalidates the `gallery` cached bundle. `scripts/generate-islands-manifest.mjs`
      post-processes the output directory and writes `dist/islands/manifest.json` with
      `{entrypoint, chunks: {island-name → /islands/browser/chunk-HASH.js}}` for the
      SSR server to inject precise `<link rel="modulepreload">` tags. `package.json`
      scripts: `build:islands` (production) and `build:islands:dev` (unminified,
      unhashed, source maps).)

## Phase 2 — Island buildout (with `iped-webui-server` Phase 2)
- [x] All remaining islands migrated to `IslandBase`; all outputs converted from Angular
      `EventEmitter` to typed `CustomEvent` dispatch via `this.dispatch()`.
      (`GalleryComponent`, `GraphComponent`, `HexViewerComponent`, `TimelineComponent`
      all extend `IslandBase`. `@Output` declarations removed from all four. Gallery
      uses `itemSelectedEvent`, `selectionChangedEvent`, `resultsLoadedEvent`,
      `similarImageSearchEvent`. Graph uses `itemSelectedEvent`. Timeline uses
      `timeRangeSelectedEvent`. HexViewer uses `islandErrorEvent` on fetch failure.)
- [x] `events.ts` extended with `timeRangeSelectedEvent` and `similarImageSearchEvent`.
      `ItemSelectedDetail.itemId` changed from `number` to `string` (composite
      `{sourceId}:{docId}` form used by gallery and graph). `SelectionChangedDetail`
      similarly uses `string[]`. Results-grid call sites updated to match.
- [x] Viewer-host island (hex/text/image/pdf rendering against viewer endpoints).
      (Completed in Phase 5 — see below.)
- [ ] Map island or SSR-rendered map — decide with `iped-geo` Phase 2.

## Phase 3 — SPA retirement
- [x] Remove the SPA `build` and `serve` architect targets from `angular.json`.
      (`npm run build` no longer builds the SPA shell; only `build:islands` /
      `build:islands:dev` are active. `src/app/app.routes.ts` replaced with an empty
      routes array — the broken lazy-load of `WorkspacePage` is gone.)
- [ ] Delete `src/app/domains/` tree — dead code with no remaining importers.
      Run: `git rm -r iped-webui/src/app/domains`
- [ ] Remove dead SPA dependencies from `package.json` once the domains tree is deleted:
      `@angular/router`, `@angular/forms` (not imported by any island).

## Phase 4 — Quality bar
- [x] Component tests per island (attribute changes, event emission, error states).
      (`results-grid.component.spec.ts`, `hex-viewer.component.spec.ts`,
      `timeline.component.spec.ts`, `gallery.component.spec.ts`,
      `graph.component.spec.ts` — TestBed + `HttpTestingController` for each island.
      Cover: initial/empty state, HTTP flow (POST search → GET results), CustomEvent
      dispatch for all output events, host-event pagination, error states, and demo-data
      fallback for timeline/graph.)
- [x] Contract tests: generated OpenAPI client compiled against each spec release
      (drift fails CI).
      (`package.json` `test:contract` script runs `tsc --noEmit` over the app sources,
      catching type drift between island call sites and generated client types.
      `generate:python` script wraps `openapi-generator-cli` for the Python client.)
- [x] Accessibility pass (keyboard navigation in grid/viewer islands).
      (`results-grid.component.html` — `<tr>` rows get `role="row"`, `tabindex="0"`,
      `aria-selected`, `aria-label`, `(keydown.enter)`, `(keydown.space)` bindings.
      `gallery.component.html` — `<div class="gallery-cell">` gets `role="gridcell"`,
      `tabindex="0"`, `aria-selected`, `aria-label`, same keyboard handlers.
      Thumbnail `<img>` `alt` changed from `""` to `"Thumbnail of {filename}".)`

## Phase 5 — Viewer island and event contract extension
- [x] Viewer-host island (`<iped-viewer>`) — MIME-type routing to sub-renderers.
      (`ViewerComponent` — extends `IslandBase`; inputs: `item-id`, `media-type`,
      `api-base`, `highlight`. Routes by MIME to five rendering modes: `text` (fetches
      `TextV2` with `Accept: text/plain`), `html` (fetches with `Accept: text/html`,
      renders via `[innerHTML]`), `image` (direct `<img>` from `ContentV2` URL),
      `pdf` (`<embed type="application/pdf">`), `hex` (delegates to
      `HexViewerComponent` inline — no double custom-element wrapping). Unknown/binary
      MIME types default to the hex renderer. Downloads link for `unsupported` mode.
      Dispatches `viewerReadyEvent({itemId, viewerType, mediaType})` on load.
      `previous-page`/`next-page` commands are forwarded into the embedded hex viewer.
      Registered as `<iped-viewer>` custom element in `main.ts`.)
- [x] `viewerReadyEvent` added to `events.ts` (`ViewerReadyDetail`, `ViewerType` union).
- [x] `viewer.component.spec.ts` — 14 tests covering: empty state, text/html fetch with
      Accept header, highlight query param, viewer-ready dispatch, image/pdf no-fetch,
      hex fallback for binary and empty MIME, error state + island-error dispatch, and
      viewerType MIME routing table for 10 MIME types.

## Standing rules
- Islands never own routing, auth, or page layout — that is the SSR server's job.
- All comms with the page via attributes in / CustomEvents out (`bubbles: true, composed: true`
  so the host page can listen at any ancestor).

## Progress checks
- `npm run build:islands` produces per-island hashed bundles consumed by the SSR
  manifest; island test suite green in CI.
