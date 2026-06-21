# iped-webui — Evolution Roadmap

> Module purpose: Angular 21 frontend workspace — originally the SPA shell for the new
> web UI (EPIC-WEB-01), now repositioned as the **island source** for the Spring Boot
> SSR composition root (`iped-webui-server`). See `REFACTORING_PLAN.md` at the repo root.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- [ISSUE-393](../issues/ISSUE-393-webui-islands-build-target.md) — Angular Elements islands build target — `done`
- [ISSUE-394](../issues/ISSUE-394-webui-first-island-results-grid.md) — First island: results grid — `done`
- Legacy SPA (`WorkspacePage` + domains structure) still present; OpenAPI client
  generation (`npm run generate:api`) wired to the `iped-webapi` spec.

## Phase 1 — Island library consolidation — `done`
- [ISSUE-395](../issues/ISSUE-395-webui-restructure-src-around-islands.md) — Restructure src/ around islands as the primary artifact — `done`
- [ISSUE-396](../issues/ISSUE-396-webui-shared-island-infrastructure.md) — Shared island infrastructure (attribute/event contracts, API client, design tokens) — `done`
- [ISSUE-397](../issues/ISSUE-397-webui-island-build-pipeline-per-island-hashes.md) — Island build pipeline with per-island independent hashes — `done`

## Phase 2 — Island buildout (with iped-webui-server Phase 2) — `done`
- [ISSUE-398](../issues/ISSUE-398-webui-all-islands-migrated-islandbase.md) — All remaining islands migrated to IslandBase with typed events — `done`
- [ISSUE-399](../issues/ISSUE-399-webui-events-ts-extended-time-range-similar-image.md) — events.ts extended with time-range and similar-image events — `done`
- [ISSUE-400](../issues/ISSUE-400-webui-viewer-host-island.md) — Viewer-host island (hex/text/image/pdf rendering) — `done`
- [ISSUE-401](../issues/ISSUE-401-webui-map-island-leaflet.md) — Map island — Leaflet island consuming GeoV2 GeoJSON endpoint — `done`

## Phase 3 — SPA retirement — `done`
- [ISSUE-402](../issues/ISSUE-402-webui-remove-spa-build-targets.md) — Remove SPA build/serve architect targets from angular.json — `done`
- [ISSUE-403](../issues/ISSUE-403-webui-delete-domains-tree.md) — Delete src/app/domains/ tree — `done`
- [ISSUE-404](../issues/ISSUE-404-webui-remove-dead-spa-dependencies.md) — Remove dead SPA dependencies from package.json — `done`

## Phase 4 — Quality bar — `done`
- [ISSUE-405](../issues/ISSUE-405-webui-component-tests-per-island.md) — Component tests per island — `done`
- [ISSUE-406](../issues/ISSUE-406-webui-contract-tests-openapi-client.md) — Contract tests for the generated OpenAPI client — `done`
- [ISSUE-407](../issues/ISSUE-407-webui-accessibility-pass-keyboard-nav.md) — Accessibility pass — keyboard navigation in grid/viewer islands — `done`

## Phase 5 — Viewer island and event contract extension — `done`
- [ISSUE-409](../issues/ISSUE-409-webui-viewer-host-island-mime-routing.md) — Viewer-host island (iped-viewer) — MIME-type routing to sub-renderers — `done`
- [ISSUE-410](../issues/ISSUE-410-webui-viewer-ready-event.md) — viewerReadyEvent added to events.ts — `done`
- [ISSUE-408](../issues/ISSUE-408-webui-viewer-component-spec-tests.md) — viewer.component.spec.ts test coverage — `done`

## Standing rules
- Islands never own routing, auth, or page layout — that is the SSR server's job.
- All comms with the page via attributes in / CustomEvents out (`bubbles: true, composed: true`
  so the host page can listen at any ancestor).

## Progress checks
- `npm run build:islands` produces per-island hashed bundles consumed by the SSR
  manifest; island test suite green in CI.
