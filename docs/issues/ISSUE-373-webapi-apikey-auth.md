# ISSUE-373: Pluggable API-key session/auth model

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 2 — Multi-case and session model
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add an optional API-key authentication filter so deployments can require a key while staying open by default for local lab use, with OIDC/LDAP deferred to a later phase.

## Problem

`iped-webapi` had no authentication mechanism, which is unacceptable for any deployment beyond a fully trusted local lab.

## Acceptance criteria

- [x] `ApiKeyAuthFilter.java` implemented as a Jersey `ContainerRequestFilter` at `AUTHENTICATION` priority.
- [x] Key read from `iped.webapi.api-key` system property or `IPED_WEBAPI_API_KEY` env var.
- [x] Accepts `X-Api-Key: <key>` or `Authorization: Bearer <key>`.
- [x] No-op when no key is configured (open mode for local lab); OIDC/LDAP deferred to Phase 3.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
