# iped-webui → Spring Boot SSR + HTMX + Angular Islands — Refactoring Plan & Progress

> Status: **Vertical-slice pilot COMPLETE and verified end-to-end.**
> Last updated: 2026-06-08. Stack upgraded to **Spring Boot 4**.

## Context

`iped-webui` was a freshly-scaffolded Angular 21 SPA whose entire UI lived in one
monolithic `WorkspacePage` component. The backend `iped-webapi` is Jersey/JAX-RS on
embedded Jetty — there was no Spring anywhere in the project.

This effort redirects toward a **server-owned composition root**: Spring Boot owns
routing, auth, layout, SSR (Rocker) + HTMX fragments; Angular survives only as embedded
**custom-element "islands"** mounted inside SSR pages, never as the app shell. Goal: SSR
for most pages, targeted client richness where it pays off, and a migration path toward
stronger frontend modularity without a full micro-frontend platform yet.

### Decisions (locked with the user)
- **Backend topology:** new Spring Boot module owns the pages AND re-exposes `/api/**` by
  proxying to the existing Jersey `iped-webapi` (single browser origin).
- **Scope:** vertical-slice pilot — one SSR page + one island.
- **First island:** results grid.
- **Java:** whole project bumped to JDK 25.
- **Framework:** **Spring Boot 4** (upgraded from the initial 3.5.x scaffold).

## Architecture

```
Browser  ─ full page GET /workspace ─▶ Spring MVC ─▶ Rocker page (layout + HTMX regions + island host)
         ─ HTMX fragment GET/POST   ─▶ Spring MVC ─▶ Rocker partials (HTML only)
         ─ island JSON fetch /api/** ─▶ Spring proxy ─▶ Jersey iped-webapi ─▶ iped-engine
```

Two rendering runtimes in adjacent boxes: Rocker fragments on one side, the Angular custom
element on the other. Neither patches the other's DOM subtree.

## Progress checklist

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | Bump project to JDK 25 | ✅ done | Root `pom.xml` 16→25; aligns with `iped-api`/`iped-engine-core` already at 25. Full-reactor `verify` on JDK 25 NOT yet run (separate gate). |
| 2 | Scaffold `iped-webui-server` Spring Boot module | ✅ done | Registered in aggregator pom; no IPED deps (proxy-only). |
| 3 | Rocker SSR shell + workspace page | ✅ done | `layout/main` + `workspace/page`; `WorkspaceController` serves `GET /workspace`. |
| 4 | `/api/**` proxy + temporary search stub | ✅ done | `ApiProxyController` (clean 502 when backend down) + `SearchStubController`. |
| 5 | HTMX fragment regions | ✅ done | `WorkspaceFragmentController` + sidebar/info Rocker partials (HTML only). |
| 6 | Angular results-grid island (Angular Elements) | ✅ done | `ResultsGridComponent` (zoneless, signals); attributes-in / CustomEvents-out; `islands` build target. |
| 7 | Island asset pipeline + manifest wiring | ✅ done | `frontend-maven-plugin` build → hashed bundle staged to `static/islands/`; `IslandManifest` injects it; immutable cache headers. |
| 8 | Boundary-rules README + end-to-end verification | ✅ done | Module README; 3/3 integration tests pass; verified in browser. |
| 9 | **Upgrade to Spring Boot 4** | ✅ done | `spring-boot.version` = 4.0.0 (Spring Framework 7); verified at runtime (banner `v4.0.0`) and 3/3 tests green. See migration notes below. |

### Spring Boot 4 migration notes (changes that bit us)
- **`HttpHeaders` no longer implements `MultiValueMap`** (Spring Framework 7). The proxy's
  header copy was rewritten to `incoming.forEach((name, values) -> out.addAll(name, values))`.
- **`HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE`** path resolution replaced with
  `request.getRequestURI()` minus context path — version-robust, no framework-internal attribute.
- **Test module restructuring**: `@AutoConfigureMockMvc` and `TestRestTemplate` are no longer
  pulled by `spring-boot-starter-test`. `WorkspacePilotTest` now uses `@SpringBootTest(RANDOM_PORT)`
  + the JDK `HttpClient` + the `local.server.port` env property.
- **Jackson 3** is the default (package `tools.jackson.*`, not `com.fasterxml.jackson.*`). App
  JSON works transparently; the test avoids JSON parsing and asserts on raw response bodies.

## Key files

**Backend — `iped-webui-server/`**
- `src/main/java/iped/webui/WebUiServerApplication.java` — Spring Boot entry.
- `web/WorkspaceController.java` — `GET /` → `/workspace`, full Rocker page.
- `web/WorkspaceFragmentController.java` — HTMX fragment endpoints (HTML).
- `web/ApiProxyController.java` — `/api/**` → Jersey `iped-webapi`.
- `web/SearchStubController.java` — TEMPORARY contract-shaped search JSON.
- `islands/IslandManifest.java` — resolves hashed island bundle names.
- `config/WebConfig.java`, `config/WebUiProperties.java` — static handler + RestClient + proxy target.
- `src/main/rocker/views/...` — `layout/main`, `workspace/page`, `workspace/fragments/{sidebar,infoPanel}`.
- `src/test/java/iped/webui/web/WorkspacePilotTest.java` — 3 wiring tests.
- `README.md` — boundary rules, island contract, build/run.

**Frontend — `iped-webui/`**
- `src/islands/main.ts` — Angular Elements bootstrap (zoneless).
- `src/islands/results-grid/results-grid.component.{ts,html,scss}` — the island.
- `angular.json` — `islands` build target (hashed, `index:false`, no polyfills).
- `package.json` — `@angular/elements` dep + `build:islands` script.

## Boundary rules (enforced)
- Spring owns page URLs, auth, layout, CSRF, navigation state.
- HTMX owns server-rendered fragments (forms, panels, tabs, validation).
- Angular owns only the island root + internals + JSON; SSR never patches inside it.
- No shared mutable client state — comms via DOM CustomEvents, URL/query, or backend only.
- No duplicate routing (islands have no router); no duplicate fetching of the same widget.
- Design tokens shared via CSS variables only; rendering ownership stays separate.
- **Angular Elements output events fire on the host element and do NOT bubble** — bridges
  listen on the island element, not `document`.

## Verification (done)
- `mvn -pl iped-webui-server -Dwebui.skipFrontend=true test` → 3/3 green on JDK 25.
- `npm run build:islands` → single hashed `main-*.js` (~44 kB transfer).
- Browser: SSR shell renders, HTMX loads sidebar, island mounts + renders 25 rows from the
  `/api` stub, row click → `item-selected` → HTMX re-renders the server-owned info panel.
  Grid data fetched once as JSON, not duplicated as HTML. No console errors.

## Engine build unblock (incidental, while trying to start iped-webapi)
A pre-existing break on this branch (commit `6f5b8426a "Config refactoring"`) moved
`ExportByCategoriesConfig` from `iped-engine` into `iped-tasks-forensics` but left two
references behind in `iped-engine`, so it did not compile (a layering violation — engine
cannot see the forensics module). Minimal layering-correct fix applied:
- `ExportByKeywordsConfig` now defines its own `ENABLE_PARAM` constant instead of borrowing
  the moved class's.
- `Statistics` gates on the shared enable property
  (`ConfigurationManager.getEnableTaskProperty(...)`) instead of referencing the moved class.

Result: `iped-engine` + `iped-webapi` (+ their deps) build cleanly on JDK 25. Running
`iped-webapi` still requires a **real processed IPED case** via `--sources` (it loads that
case's config + Lucene index); an empty source list trips a `ConfigurationManager` NPE.

## SPA layout reproduced in the SSR module
The full Angular SPA workspace layout was ported into the Spring Boot SSR + island module:
- Design system ported from `workspace-page.scss` + `styles.scss` into
  `iped-webui-server/src/main/resources/static/css/workspace.css` (tokens on `:root` so they
  cascade to both the SSR shell and the light-DOM island).
- `views/workspace/page.rocker.html` reproduces the `.app` grid: topbar (brand, search,
  filter chip, actions, profile), tabbed sidebar, main panel with Table/Gallery/Map/Timeline/
  Links mode switcher + paging, stacked info + viewer panels, status bar with live clock.
- New/updated HTMX fragments: `sidebar` (Categories tree + facets / Metadata / Bookmarks /
  Reports), `infoPanel` (Hits/Attachments/Parent/Duplicates/References), `viewer`
  (Hex/Text/Metadata/Preview) — all server-owned HTML.
- The Angular island renders the results table using the SPA's exact classes; global
  `workspace.css` styles it (emulated encapsulation doesn't block document styles), so it is
  pixel-consistent with the SPA without duplicating CSS.
- Boundary bridges (DOM events + attributes only): topbar search → island `query` attribute;
  island `item-selected` → HTMX info + viewer panels + status path; island `results-loaded`
  → SSR hit-count / summary / paging chrome; SSR paging buttons → island via `previous-page`/
  `next-page` CustomEvents (`@HostListener`). Verified: grid mounts (25 rows), select drives
  panels, paging updates "26–50 of 137", no console errors. 3/3 tests green.

## Outstanding / follow-ups
- Run a **full reactor `mvn verify` on JDK 25** (parsers, sleuthkit) — still a separate gate
  (engine + webapi now confirmed building).
- Replace `SearchStubController` with a real v2 search implementation against the engine
  (EPIC-WEB-03 in `specs/87-web-ui-delivery-backlog.md`).
- Add further islands (viewer, link graph) the same way; retire the SPA `WorkspacePage`
  once enough islands exist.
- Wire real auth/CSRF in `SecurityConfig` (pilot has hooks but no enforcement).
