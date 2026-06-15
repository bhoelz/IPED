# iped-webui-server — Evolution Roadmap

> Module purpose: Spring Boot 4 server-side composition root for the web UI — Rocker SSR
> pages, HTMX fragments, Angular Elements "islands", and the `/api/**` proxy to the
> Jersey `iped-webapi`. See `REFACTORING_PLAN.md` (repo root) for the pilot history.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- [x] Vertical-slice pilot complete and verified: SSR workspace shell, HTMX sidebar/info/
      viewer fragments, results-grid island, island asset pipeline, API proxy.
- [x] Spring Boot 4 / JDK 25; 3/3 integration tests green.
- `SearchStubController` still serves contract-shaped fake search data.
- Security config has hooks but no enforcement.

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
- [ ] Third island: link-graph view.
- [ ] Island manifest supports multiple bundles with independent hashing/caching.
- [ ] Retire the SPA `WorkspacePage` in `iped-webui` once islands cover its workflows.

## Phase 3 — Auth, sessions, hardening
- [x] Enforce auth/CSRF in `SecurityConfig`.
      (`SecurityConfig.java` — `@EnableWebSecurity` with cookie-based CSRF
      (`CookieCsrfTokenRepository.withHttpOnlyFalse()`); CSRF disabled for `/api/**` (Jersey
      enforces its own API-key auth). Static assets + actuator health are public. GET requests
      to HTMX fragments permitted in lab/open mode; POST/PUT/DELETE require auth. HTTP Basic
      as baseline; OIDC deferred to Phase 4.)
- [x] BFF contract tests (`WorkspaceBffContractTest`).
      (`@WebMvcTest` + `MockRestServiceServer` — 8 tests covering: v2/bookmarks API call on
      sidebar coll tab, demo fallback on API error, `<iped-viewer>` emission with `media-type`
      in preview mode, graceful degradation when API down, meta tab metadata table, hex mode
      zero-backend-calls, no-item-id empty state, categories happy path and fallback.)
- [x] CSP and security headers tuned for evidence-content rendering.
      (`SecurityConfig` extended with `contentSecurityPolicy()`, `httpStrictTransportSecurity()`,
      `referrerPolicy(SAME_ORIGIN)`, `frameOptions(DENY)`, `xssProtection(ENABLED_MODE_BLOCK)`,
      `permissionsPolicy()`. CSP allows `frame-src 'self'` for HTML rendition viewer and
      `object-src 'self'` for PDF `<embed>`. `'unsafe-inline'` styles-only for Rocker SSR.
      `data:` and `blob:` for gallery thumbnails and blob URLs.)
- [ ] Full reactor `mvn verify` on JDK 25 as a CI gate (still outstanding from the pilot).

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
