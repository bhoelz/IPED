# ISSUE-384: Per-source access control list

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 6 — Per-source ACL and item write endpoints
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a per-source allow-list so multi-tenant deployments can restrict which sources/cases a given API key or client is permitted to see or operate on.

## Problem

Without per-source access control, any authenticated client could see and operate on every open case/source, which is unacceptable in multi-tenant deployments.

## Acceptance criteria

- [x] `iped.webapi.allowed-sources` (or `IPED_WEBAPI_ALLOWED_SOURCES` env var) — comma-separated allow-list of source IDs.
- [x] `AllowedSources.java` resolves at class-load time, cached; `parse()` package-private for unit testing; `overrideForTest`/`resetToConfigured` allow test isolation.
- [x] `SourceAccessFilter.java` — Jersey `ContainerRequestFilter` at `AUTHORIZATION + 1` priority intercepting `/v2/sources/{sourceId}/…`, `/sources/{sourceID}/…` (v1), and `/v2/cases/{id}/…`; returns 403 for unlisted IDs.
- [x] `CasesV2.listCases()` filters the case list by `AllowedSources`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
