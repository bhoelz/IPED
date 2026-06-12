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
- [ ] Remove `SearchStubController`; proxy to real `iped-webapi` v2 search
      (blocks on EPIC-WEB-03 — see `iped-webapi/ROADMAP.md` Phase 1).
- [ ] Wire info panel, viewer fragments, and sidebar trees to real endpoints (today they
      render static/stub HTML).
- [ ] Error/empty/loading states for every fragment when the backend is down (the proxy
      already returns clean 502s — surface them properly in the UI).

## Phase 2 — Island growth
- [ ] Second island: viewer host (hex/text/image/pdf) following the established
      boundary rules (attributes in, CustomEvents out, no shared client state).
- [ ] Third island: link-graph view.
- [ ] Island manifest supports multiple bundles with independent hashing/caching.
- [ ] Retire the SPA `WorkspacePage` in `iped-webui` once islands cover its workflows.

## Phase 3 — Auth, sessions, hardening
- [ ] Enforce auth/CSRF in `SecurityConfig` (pilot has hooks only); align the session
      model with `iped-webapi` Phase 2 auth.
- [ ] CSP and security headers tuned for evidence-content rendering (coordinate with
      `iped-viewers` sanitization work).
- [ ] Full reactor `mvn verify` on JDK 25 as a CI gate (still outstanding from the pilot).

## Phase 4 — Production posture (5.0)
- [ ] Packaging: single deployable serving SSR + islands + proxied API, documented in
      `DEPLOYMENT-GUIDE.md`; container image in the docker stack.
- [ ] Decide long-term topology with `iped-webapi` (proxy-forever vs consolidation) —
      shared decision gate, tracked in both roadmaps.
- [ ] Observability: request tracing across proxy → Jersey → engine.

## Boundary rules (standing, enforced)
- Spring owns URLs/auth/layout; HTMX owns server-rendered fragments; Angular owns only
  island internals; communication via DOM events/attributes/backend only.

## Progress checks
- Zero stub controllers; integration tests cover each fragment against a real case.
- Browser workflow (search → select → view) works end-to-end on a processed case.
