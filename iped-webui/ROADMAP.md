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
- [ ] Restructure `src/` around islands as the primary artifact (`src/islands/*`);
      mark the SPA shell as legacy (no new features).
- [ ] Shared island infrastructure: typed attribute/event contracts, generated API
      client usage inside islands, design tokens via CSS variables only (no SCSS theme
      coupling to the SSR shell).
- [ ] Island build pipeline: per-island bundles with independent hashes (today: single
      bundle) so adding islands doesn't bust all caches.

## Phase 2 — Island buildout (with `iped-webui-server` Phase 2)
- [ ] Viewer-host island (hex/text/image/pdf rendering against viewer endpoints).
- [ ] Link-graph island (visualization of `iped-engine-graph` data).
- [ ] Gallery island (virtualized thumbnail grid — the workload that justifies
      client-side richness).
- [ ] Map island or SSR-rendered map — decide with `iped-geo` Phase 2.

## Phase 3 — SPA retirement
- [ ] Once islands cover the SPA workspace workflows, delete `WorkspacePage` and unused
      domain modules; the workspace keeps only islands + generated client + shared libs.
- [ ] Remove dead dependencies and the SPA build target; CI builds islands only.

## Phase 4 — Quality bar
- [ ] Component tests per island (attribute changes, event emission, error states).
- [ ] Contract tests: generated OpenAPI client compiled against each spec release
      (drift fails CI).
- [ ] Accessibility pass (keyboard navigation in grid/viewer islands).

## Standing rules
- Islands never own routing, auth, or page layout — that is the SSR server's job.
- All comms with the page via attributes in / CustomEvents out (events do not bubble —
  listeners attach on the host element).

## Progress checks
- `npm run build:islands` produces per-island hashed bundles consumed by the SSR
  manifest; island test suite green in CI.
