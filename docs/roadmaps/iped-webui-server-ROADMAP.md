# iped-webui-server — Evolution Roadmap

> Module purpose: Spring Boot 4 server-side composition root for the web UI — Rocker SSR
> pages, HTMX fragments, Angular Elements "islands", and the `/api/**` proxy to the
> Jersey `iped-webapi`. See `REFACTORING_PLAN.md` (repo root) for the pilot history.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06) — `done`
- [ISSUE-414](../issues/ISSUE-414-webui-server-pilot-complete.md) — Vertical-slice pilot complete and verified — `done`
- [ISSUE-415](../issues/ISSUE-415-webui-server-spring-boot4-jdk25.md) — Spring Boot 4 / JDK 25; all integration tests green — `done`
- [ISSUE-416](../issues/ISSUE-416-webui-server-auth-enforced.md) — Auth fully enforced (form login, pilot user, CSRF, HTMX bridge) — `done`
- [ISSUE-417](../issues/ISSUE-417-webui-server-real-webapi-data-wired.md) — Real webapi data wired for categories, bookmarks, search, metadata, viewer — `done`

## Phase 1 — Real data end-to-end — `done`
- [ISSUE-418](../issues/ISSUE-418-webui-server-remove-search-stub-controller.md) — Remove SearchStubController; proxy to real webapi v2 search — `done`
- [ISSUE-419](../issues/ISSUE-419-webui-server-wire-info-viewer-sidebar-real-endpoints.md) — Wire info panel, viewer fragments, sidebar trees to real endpoints — `done`
- [ISSUE-420](../issues/ISSUE-420-webui-server-error-empty-loading-states.md) — Error/empty/loading states for every fragment — `done`

## Phase 2 — Island growth — `done`
- [ISSUE-421](../issues/ISSUE-421-webui-server-second-island-viewer.md) — Second island: `<iped-viewer>` MIME-routed renderer — `done`
- [ISSUE-422](../issues/ISSUE-422-webui-server-third-island-link-graph.md) — Third island: link-graph view — `done`
- [ISSUE-423](../issues/ISSUE-423-webui-server-island-manifest-multi-bundle.md) — Island manifest supports multiple independently-hashed bundles — `done`
- [ISSUE-424](../issues/ISSUE-424-webui-server-retire-spa-workspacepage.md) — Retire the SPA WorkspacePage in iped-webui — `done`

## Phase 3 — Auth, sessions, hardening — `done`
- [ISSUE-425](../issues/ISSUE-425-webui-server-enforce-auth-csrf.md) — Enforce auth/CSRF in SecurityConfig — `done`
- [ISSUE-426](../issues/ISSUE-426-webui-server-bff-contract-pilot-tests.md) — BFF contract tests + pilot tests — `done`
- [ISSUE-427](../issues/ISSUE-427-webui-server-csp-security-headers.md) — CSP and security headers for evidence-content rendering — `done`
- [ISSUE-428](../issues/ISSUE-428-webui-server-ci-gate-jdk25-angular.md) — Full reactor `mvn verify` on JDK 25 as CI gate; Angular island tests in CI — `done`

## Phase 4 — Production posture (5.0) — `in_progress`
- [ISSUE-429](../issues/ISSUE-429-webui-server-packaging-deployment-guide.md) — Packaging: single deployable documented in a DEPLOYMENT-GUIDE.md — `done`
- [ISSUE-430](../issues/ISSUE-430-webui-server-topology-decision.md) — Decide long-term topology with iped-webapi — `done`
- [ISSUE-431](../issues/ISSUE-431-webui-server-observability-request-tracing.md) — Observability: request tracing across proxy → Jersey → engine — `done`

## Phase 5 — Security hardening — `planned`
- [ISSUE-446](../issues/ISSUE-446-fix-onclick-js-string-xss.md) — Fix DOM event-handler XSS in Rocker templates (onclick string interpolation) — `done`

## Boundary rules (standing, enforced)
- Spring owns URLs/auth/layout; HTMX owns server-rendered fragments; Angular owns only
  island internals; communication via DOM events/attributes/backend only.

## Progress checks
- Zero stub controllers; integration tests cover each fragment against a real case.
- Browser workflow (search → select → view) works end-to-end on a processed case.
