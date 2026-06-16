# iped-webui-server — Evolution Roadmap

> Module purpose: Spring Boot 4 server-side composition root for the web UI — Rocker SSR
> pages, HTMX fragments, Angular Elements "islands", and the `/api/**` proxy to the
> Jersey `iped-webapi`. See `REFACTORING_PLAN.md` (repo root) for the pilot history.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Vertical-slice pilot complete and verified: SSR workspace shell, HTMX sidebar/info/
      viewer fragments, results-grid island, island asset pipeline, API proxy.
- [x] Spring Boot 4 / JDK 25; all integration tests green.
- [x] Auth fully enforced (form login, pilot user, CSRF, HTMX bridge).
- [x] Real webapi data wired for categories, bookmarks (with counts), search, metadata, viewer.

## Phase 1 — Real data end-to-end
- [x] Remove `SearchStubController`; proxy to real `iped-webapi` v2 search
      (blocks on EPIC-WEB-03 — see `iped-webapi/ROADMAP.md` Phase 1).
      (`SearchStubController` deleted. `SearchBffController` added at the same
      `POST /api/cases/{caseId}/search` + `GET …/{searchId}/results` URLs. It encodes
      the query into a stateless Base64 `searchId` token and proxies to
      `GET /v2/search?q=…&sourceId={caseId}&offset=&limit=` on iped-webapi, then
      re-shapes the response to match the island's two-step contract. HTTP 503 from
      webapi (no cases open) is relayed cleanly so the island can show a "no cases
      open" state rather than an empty grid.)
- [x] Wire info panel, viewer fragments, and sidebar trees to real endpoints (today they
      render static/stub HTML).
      (Sidebar "Categories" tab now calls `GET /v2/sources/{caseId}/items/categories`.
      Sidebar "Bookmarks" calls `/bookmarks` (v1). Viewer "meta" tab and info panel
      metadata now call `GET /v2/sources/{src}/items/{id}` (ItemMetadataJSON) and
      flatten structured fields + Tika metadata map for display.)
- [x] Error/empty/loading states for every fragment when the backend is down (the proxy
      already returns clean 502s — surface them properly in the UI).
      (`FetchResult<T>(data, fromBackend)` typed wrapper distinguishes live vs demo data.
      When `fromBackend=false`, `sidebar()` prepends a `backendUnavailableBanner()` HTML
      snippet above the tab content. `viewer()` meta tab prepends `itemErrorFragment()`
      when metadata couldn't be retrieved. `SearchBffController` returns HTTP 503/502
      with a JSON error body so the island grid can surface "backend unavailable" in its
      own error state rather than showing an empty list.)
      Also: `IslandManifest` updated to read `manifest.json` (from
      `generate-islands-manifest.mjs`) for per-island `chunkFor(name)` lookups;
      `pom.xml` copies manifest.json alongside the browser bundles.

## Phase 2 — Island growth
- [x] Second island: `<iped-viewer>` — auto-routes by MIME type to text/html/image/pdf/hex/
      unsupported renderers; delegates to `<iped-hex-viewer>` for binary/unknown content.
      (`viewer.rocker.html` updated to use `<iped-viewer item-id media-type api-base>`;
      `WorkspaceFragmentController.viewer()` now fetches metadata for both "meta" and
      "preview" modes to extract `mediaType` and pass it to the template.
      `fetchBookmarks()` updated to call `GET /v2/bookmarks` (v2 API, key `bookmarks`).)
- [x] Third island: link-graph view.
      (`<iped-graph case-id api-base>` registered as `iped-graph` custom element in
      `iped-webui/src/islands/main.ts`; `GraphComponent` migrated to `IslandBase` in
      iped-webui Phase 2. SSR workspace page (`page.rocker.html`) includes the island
      at `<iped-graph data-view="graph" hidden>` alongside the mode button
      `Links` → `ipedSetMode('graph')`. No SSR fragment needed — the island owns its
      own graph rendering via `api-base`.)
- [x] Island manifest supports multiple bundles with independent hashing/caching.
      (`IslandManifest.java` loads `manifest.json` (written by `generate-islands-manifest.mjs`)
      and exposes `chunkFor(islandName)` → hashed chunk URL for targeted
      `<link rel="modulepreload">` injection. Each island chunk is independently hashed;
      updating one island does not bust others' cached bundles.)
- [x] Retire the SPA `WorkspacePage` in `iped-webui` once islands cover its workflows.
      (`src/app/domains/` deleted (12 files); `@angular/router`, `@angular/forms` removed
      from `package.json`; `app.ts` and `app.config.ts` stripped of router imports.
      Completed in iped-webui Phase 3.)

## Phase 3 — Auth, sessions, hardening
- [x] Enforce auth/CSRF in `SecurityConfig`.
      (Full auth enforcement: all routes require `authenticated()` except static assets,
      actuator probes, and `/api/**` proxy. Form login at `/login` (served by `LoginController`
      + `views/login/page.rocker.html`) with `defaultSuccessUrl("/cases")`. HTTP Basic kept for
      CLI clients. Logout at `POST /logout` → `/login?logout`. Built-in pilot account via
      `spring.security.user.*` (`analyst`/`{noop}iped`). `DemoDataController` gated behind
      `@Profile("demo")`; activated by `spring.profiles.active: demo` in `application.yml`.
      CSRF: `CookieCsrfTokenRepository.withHttpOnlyFalse()`; excluded for `/api/**`, `/login`,
      `/logout`, `/cases/**`. HTMX bridge in `main.rocker.html` injects `X-XSRF-TOKEN` from
      cookie on all mutating HTMX requests. `caseId` now threaded to every fragment endpoint;
      `WorkspaceFragmentController` falls back to `filterState.getActiveCaseId()` when param
      is absent.)
- [x] BFF contract tests (`WorkspaceBffContractTest`) + pilot tests (`WorkspacePilotTest`).
      (18 tests total. Both classes use `@SpringBootTest(webEnvironment=MOCK)` +
      `MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())` —
      `@AutoConfigureMockMvc` was removed in Spring Boot 4. `@WithMockUser` at class level
      authenticates all requests through the now-enforced security filter chain.)
- [x] CSP and security headers tuned for evidence-content rendering.
      (`SecurityConfig` extended with `contentSecurityPolicy()`, `httpStrictTransportSecurity()`,
      `referrerPolicy(SAME_ORIGIN)`, `frameOptions(DENY)`, `xssProtection(ENABLED_MODE_BLOCK)`,
      `permissionsPolicy()`. CSP allows `frame-src 'self'` for HTML rendition viewer,
      `object-src 'self'` for PDF `<embed>`, `style-src`/`font-src` for Google Fonts
      (IBM Plex Sans/Mono used on login and case-picker pages).)
- [x] Full reactor `mvn verify` on JDK 25 as a CI gate; Angular island tests in CI.
      (`build-java25-webservices` job: Temurin JDK 25, `mvn verify --also-make` over the
      web-services stack, `-Dwebui.skipFrontend=true` so Maven does not require Node.js.
      `build-angular-islands` job: Node.js 22, `npm ci` in `iped-webui/`, TypeScript contract
      check (`npm run test:contract`), then Vitest island specs (`npm test`; CI=true env var
      triggers non-watch mode automatically.)

## Phase 4 — Production posture (5.0)
- [x] Packaging: single deployable documented in `DEPLOYMENT-GUIDE.md`.
      (`DEPLOYMENT-GUIDE.md` added: build commands, configuration reference, nginx TLS termination
      example, Docker/Compose quick-start, observability section, security notes, and the formal
      topology decision — proxy-forever vs consolidation.)
- [x] Decide long-term topology with `iped-webapi` (proxy-forever vs consolidation).
      (Decision: **keep two-process model** for 5.0. Engine JVM co-loads Lucene + pipeline;
      merging classpath into Spring Boot would conflict. Documented in `DEPLOYMENT-GUIDE.md`
      with the future consolidation trigger: only if `iped-webapi` decouples from the engine JVM.)
- [x] Observability: request tracing across proxy → Jersey → engine.
      (`ProxyTracingFilter` — `@Order(1)` `OncePerRequestFilter`. Assigns `X-Trace-Id` (UUID,
      32 hex chars) per request; reuses existing header from upstream gateway if present. Puts
      `traceId` in MDC so all log lines carry it. `ApiProxyController` forwards the header to
      Jersey. Response carries the same header for browser-console correlation. Log4j2 pattern
      note and Spring Boot 4 structured-logging hint documented in `DEPLOYMENT-GUIDE.md`.)

## Boundary rules (standing, enforced)
- Spring owns URLs/auth/layout; HTMX owns server-rendered fragments; Angular owns only
  island internals; communication via DOM events/attributes/backend only.

## Progress checks
- Zero stub controllers; integration tests cover each fragment against a real case.
- Browser workflow (search → select → view) works end-to-end on a processed case.
