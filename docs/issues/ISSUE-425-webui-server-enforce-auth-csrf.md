# ISSUE-425: Enforce auth/CSRF in SecurityConfig

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 3 — Auth, sessions, hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

All routes require `authenticated()` except static assets, actuator probes, and `/api/**`; form login lives at `/login` with `defaultSuccessUrl("/cases")`, HTTP Basic is kept for CLI clients, and CSRF uses `CookieCsrfTokenRepository.withHttpOnlyFalse()` with the HTMX bridge injecting `X-XSRF-TOKEN` on mutating requests.

## Problem

Earlier pilot work left auth/CSRF only partially enforced; production posture requires it applied consistently across all routes including HTMX fragment endpoints.

## Acceptance criteria

- [x] All non-static/non-actuator/non-proxy routes require authentication.
- [x] Form login at `/login`, logout at `POST /logout`.
- [x] Built-in pilot account configured via `spring.security.user.*`.
- [x] CSRF cookie repository configured; excluded only for `/api/**`, `/login`, `/logout`, `/cases/**`.
- [x] HTMX bridge injects `X-XSRF-TOKEN` from cookie on mutating requests.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
