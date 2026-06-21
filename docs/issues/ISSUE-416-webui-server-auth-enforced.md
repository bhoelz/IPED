# ISSUE-416: Auth fully enforced (form login, pilot user, CSRF, HTMX bridge)

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Current state
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Authentication is fully enforced across the application: form login, a pilot user account, CSRF protection, and an HTMX bridge that injects the CSRF token on mutating requests.

## Problem

The pilot initially ran without enforced auth; production-readiness requires authentication and CSRF protection on every route, including HTMX-driven fragment requests.

## Acceptance criteria

- [x] Form login enforced for all routes except static/actuator/login.
- [x] CSRF protection active, with HTMX bridge injecting the token on mutating requests.
- [x] Pilot user account available for testing.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
