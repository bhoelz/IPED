# iped-webapi — Evolution Roadmap

> Module purpose: Jersey/JAX-RS REST API on embedded Jetty exposing case open/search/
> item/viewer capabilities of the engine (`iped.engine.webapi`, JSON models incl. `v2`).
> The single backend contract for `iped-webui`, `iped-webui-server`, and `iped-mcp`.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- v1 endpoints plus an emerging `json/v2` model; OpenAPI draft exists
  (`specs/84-phase-1-openapi-initial.yaml`); the Angular client is generated from it.
- Requires a real processed case at startup (`--sources`); empty source list NPEs in
  `ConfigurationManager` — known rough edge.
- Web UI currently leans on a temporary search stub in `iped-webui-server`
  (EPIC-WEB-03 in `specs/87-web-ui-delivery-backlog.md`).

## Phase 1 — v2 contract completion — `done`
- [ISSUE-368](../issues/ISSUE-368-webapi-v2-real-search.md) — Implement real v2 search against the engine — `done`
- [ISSUE-369](../issues/ISSUE-369-webapi-v2-item-content-metadata-endpoints.md) — Item retrieval, content preview, metadata/facets, category/bookmark trees — `done`
- [ISSUE-370](../issues/ISSUE-370-webapi-startup-hardening-zero-sources.md) — Startup hardening — validate --sources, support zero-case startup — `done`
- [ISSUE-371](../issues/ISSUE-371-webapi-search-pagination-bounds.md) — Pagination/cursor semantics bounded for large result sets — `done`

## Phase 2 — Multi-case and session model — `done`
- [ISSUE-372](../issues/ISSUE-372-webapi-multi-case-open-close.md) — Open/close multiple cases per server instance — `done`
- [ISSUE-373](../issues/ISSUE-373-webapi-apikey-auth.md) — Pluggable API-key session/auth model — `done`
- [ISSUE-374](../issues/ISSUE-374-webapi-audit-logging-mutations.md) — Audit logging of all mutating operations — `done`

## Phase 3 — Jobs and streaming — `done`
- [ISSUE-375](../issues/ISSUE-375-webapi-async-job-api.md) — Async job API for export and report generation — `done`
- [ISSUE-376](../issues/ISSUE-376-webapi-sse-job-progress.md) — SSE channel for real-time job progress — `done`
- [ISSUE-377](../issues/ISSUE-377-webapi-viewer-content-byte-range.md) — Viewer content endpoints with byte-range support — `done`

## Phase 4 — Platform (5.0) — `done`
- [ISSUE-378](../issues/ISSUE-378-webapi-content-format-conversion.md) — On-the-fly content format conversion (PNG, WebM transcode) — `done`
- [ISSUE-379](../issues/ISSUE-379-webapi-v1-deprecation-policy.md) — v1 versioning/deprecation policy and headers — `done`
- [ISSUE-380](../issues/ISSUE-380-webapi-generated-python-client.md) — Generated Python client from the OpenAPI spec — `done`
- [ISSUE-381](../issues/ISSUE-381-webapi-jersey-spring-decision.md) — Jersey-vs-Spring stack consolidation decision — `done`

## Phase 5 — Bookmark CRUD and stack consolidation — `done`
- [ISSUE-382](../issues/ISSUE-382-webapi-bookmark-crud-v2.md) — Bookmark CRUD v2 REST layer — `done`
- [ISSUE-383](../issues/ISSUE-383-webapi-jersey-spring-consolidation-recorded.md) — Record Jersey-vs-Spring consolidation decision and stack boundaries — `done`

## Phase 6 — Per-source ACL and item write endpoints — `done`
- [ISSUE-384](../issues/ISSUE-384-webapi-per-source-acl.md) — Per-source access control list — `done`
- [ISSUE-385](../issues/ISSUE-385-webapi-item-tag-endpoints.md) — Item tag endpoints (bookmark-backed, REST-ergonomic) — `done`
- [ISSUE-386](../issues/ISSUE-386-webapi-item-selection-write-v2.md) — Item selection write v2 endpoint — `done`
- [ISSUE-387](../issues/ISSUE-387-webapi-closes-mcp-shared-acl-item.md) — Close deferred iped-mcp shared access-control item — `done`

## Phase 7 — Geo and format conversion — `done`
- [ISSUE-388](../issues/ISSUE-388-webapi-geojson-endpoint.md) — GeoJSON endpoint for geo-tagged items — `done`
- [ISSUE-389](../issues/ISSUE-389-webapi-format-conversion-cross-reference.md) — Cross-reference on-the-fly format conversion in Phase 7 — `done`

## Progress checks
- OpenAPI spec is the source of truth: CI fails on spec/implementation drift.
- Browser UI runs against real endpoints with zero stub controllers active.
