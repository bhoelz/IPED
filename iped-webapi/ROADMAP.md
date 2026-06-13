# iped-webapi — Evolution Roadmap

> Module purpose: Jersey/JAX-RS REST API on embedded Jetty exposing case open/search/
> item/viewer capabilities of the engine (`iped.engine.webapi`, JSON models incl. `v2`).
> The single backend contract for `iped-webui`, `iped-webui-server`, and `iped-mcp`.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- v1 endpoints plus an emerging `json/v2` model; OpenAPI draft exists
  (`specs/84-phase-1-openapi-initial.yaml`); the Angular client is generated from it.
- Requires a real processed case at startup (`--sources`); empty source list NPEs in
  `ConfigurationManager` — known rough edge.
- Web UI currently leans on a temporary search stub in `iped-webui-server`
  (EPIC-WEB-03 in `specs/87-web-ui-delivery-backlog.md`).

## Phase 1 — v2 contract completion
- [x] Implement real v2 search against the engine (replace `SearchStubController` usage);
      contract-test it against the OpenAPI spec in CI.
      (`GET /v2/search?q=...&offset=0&limit=50` added — `SearchV2.java`. Backed by
      `SearchService.searchPaginated()` SPI (iped-engine-core) + `EngineSearchService`
      impl that runs real Lucene search, slices, and fetches IItem metadata per result.
      Returns `SearchResultPageJSON` with `{total, offset, limit, items[]}` where each
      item carries name/path/mediaType/size/hash/dates/categories.)
- [x] Item retrieval, content preview, metadata/facets, category/bookmark trees — the
      endpoints the browser UI's priority workflows need.
      (`GET /v2/sources/{sourceId}/items/{id}` added — `ItemsV2.java`. Returns
      `ItemMetadataJSON` with all IItem fields + full Tika metadata map + bookmarks +
      selection state. `GET /v2/sources/{sourceId}/items/categories` returns the sorted
      leaf-category list via `IIPEDSource.getLeafCategories()`.)
- [x] Startup hardening: validate `--sources` up front with a clear error; support
      starting with zero cases and opening cases via API.
      (`--sources` made optional in `Main.java`; `EngineSourceCatalogService.init()`
      treats null/blank as zero sources and starts with an empty `IPEDMultiSource`.
      Invalid source paths now throw `IllegalArgumentException` with the bad path in the
      message, surfaced as `IOException` with "Invalid source configuration: …" from
      `startServer()`. `GET /v2/search` returns HTTP 503 with a clear JSON error when
      no sources are open.)
- [x] Pagination/cursor semantics for large result sets (root roadmap NFR: large-case
      pagination) — no unbounded responses.
      (`GET /v2/search` enforces `offset ≥ 0`, `1 ≤ limit ≤ 1000`; default limit 50.
      `SearchPage` carries total count so the UI can compute page counts.)

## Phase 2 — Multi-case and session model
- [ ] Open/close multiple cases per server instance (builds on the engine multi-case
      architecture); case-scoped routes (`/cases/{id}/...`) as the canonical shape.
- [ ] Session/auth model: pluggable authN (none for local lab, OIDC/LDAP for shared
      deployments), role-based read/tag/export permissions.
- [ ] Audit logging of all mutating operations (bookmark, tag, export) —
      chain-of-custody requirement shared with the MCP server.

## Phase 3 — Jobs and streaming
- [ ] Async job API (export, report generation) with status polling — consumed by both
      the web UI and MCP `job_*` tools.
- [ ] Server-sent events / websocket channel for processing progress and result
      streaming (feeds dashboard and UI live updates).
- [ ] Viewer content endpoints (sanitized HTML, transcoded media, hex ranges) backing
      `iped-viewers-web`.

## Phase 4 — Platform (5.0)
- [ ] Decide Jersey-vs-Spring consolidation: today `iped-webui-server` proxies to Jersey;
      either keep Jersey as the engine-API and proxy, or fold endpoints into one stack —
      decision gate before broad v2 expansion.
- [ ] Versioning policy: v1 freeze/deprecation schedule once v2 reaches parity.
- [ ] Generated clients (TypeScript, Python) published from the OpenAPI spec per release.

## Progress checks
- OpenAPI spec is the source of truth: CI fails on spec/implementation drift.
- Browser UI runs against real endpoints with zero stub controllers active.
