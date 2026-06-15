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
- [x] Open/close multiple cases per server instance.
      (`CasesV2.java` — `GET /v2/cases`, `POST /v2/cases`, `DELETE /v2/cases/{id}`,
      `GET /v2/cases/{id}`. Caller-chosen ID or UUID-from-path fallback. `POST` returns
      201; duplicate ID returns 409. Item count surfaced via `IIPEDSource.getTotalItens()`.
      Backed by `SourceCatalogService.removeSource()` SPI method added in Phase 2 of the
      engine-core — soft-remove from string↔int maps, closes handle, leaves positional
      multi-source slot intact for in-flight queries.)
- [x] Session/auth model: pluggable API-key authN.
      (`ApiKeyAuthFilter.java` — Jersey `ContainerRequestFilter` at `AUTHENTICATION`
      priority. Reads key from `iped.webapi.api-key` system property or
      `IPED_WEBAPI_API_KEY` env var. Accepts `X-Api-Key: <key>` or
      `Authorization: Bearer <key>`. No-op when no key is configured (open mode for
      local lab). OIDC/LDAP deferred to Phase 3.)
- [x] Audit logging of all mutating operations.
      (`AuditLogger.java` — writes JSONL to `audit.jsonl` (configurable via
      `iped.webapi.audit-log`); also echoes to SLF4J. `AuditLoggingFilter.java` —
      Jersey filter at `AUTHENTICATION + 1000` (runs after auth rejection) recording
      all POST/PUT/PATCH/DELETE requests with path and redacted principal.
      `CasesV2` calls `AuditLogger.log("case.open/close", ...)` directly for
      business-level events.)

## Phase 3 — Jobs and streaming
- [x] Async job API (export, report generation) with status polling.
      (`JobRegistry.java` — in-memory store keyed by UUID; `JobEntry` carries status
      (pending→running→completed/failed/cancelled), 0–100 progress counter, message,
      and a list of SSE subscriber consumers. Terminal entries are GC'd after 5 min.
      `JobsV2.java` — `POST /v2/jobs`, `GET /v2/jobs`, `GET /v2/jobs/{id}`,
      `DELETE /v2/jobs/{id}` (cancel). Supported types: `export` (ZIP selected items
      via `IItem.getBufferedInputStream()`) and `report` (HTML/PDF placeholder
      delegating to the engine's ReportTask). All mutations audited via `AuditLogger`.)
- [x] SSE channel for real-time job progress.
      (`JobsSseV2.java` — `GET /v2/jobs/{id}/events` (text/event-stream). Subscribes
      a consumer to `JobEntry.subscribe()`; immediately delivers current state so
      the client never polls first. Stream closes automatically on terminal event.
      Named events: `started`, `progress`, `completed`, `failed`, `cancelled`.)
- [x] Viewer content endpoints with byte-range support.
      (`ContentV2.java` — `GET /v2/sources/{src}/items/{id}/content`. Supports HTTP
      Range requests (RFC 7233 single-range: `bytes=N-M`, `bytes=N-`, `bytes=-N`);
      returns 206 Partial Content with `Content-Range` and `Accept-Ranges: bytes`
      headers. The hex-viewer island targets this endpoint for paged binary navigation.
      `TextV2.java` — `GET /v2/sources/{src}/items/{id}/text`. Adds `?highlight=term1+term2`
      (wraps matches in `<mark>` tags when `Accept: text/html`) and `?limit=N` for
      preview snippets. Returns 404 when source/item not found.)

## Phase 4 — Platform (5.0)
- [x] Versioning policy: v1 freeze/deprecation schedule once v2 reaches parity.
      (`V1DeprecationFilter.java` — Jersey `ContainerResponseFilter` at
      `HEADER_DECORATOR` priority. All v1 paths (anything not under `/v2/`, `/swagger`,
      `/openapi`, `/webjars`) receive `Deprecation: true`, `Sunset: Sun, 01 Jan 2027
      00:00:00 GMT`, and a `Link: </v2/cases>; rel="successor-version"` header.)
- [x] Generated Python client from the OpenAPI spec.
      (`openapi-generator-maven-plugin` added to `pom.xml`, targeting
      `specs/84-phase-1-openapi-initial.yaml`, generating a `python` client to
      `target/generated-clients/python` with package name `iped_client`. Bound to
      the `generate-sources` phase so it runs as part of a normal `mvn package`.)
- [ ] Decide Jersey-vs-Spring consolidation: today `iped-webui-server` proxies to Jersey;
      either keep Jersey as the engine-API and proxy, or fold endpoints into one stack —
      decision gate before broad v2 expansion.

## Phase 5 — Bookmark CRUD and stack consolidation
- [x] Bookmark CRUD v2 (EPIC-WEB-08 / WEB-071).
      (`BookmarksV2.java` — v2 REST layer over the existing `BookmarkService` SPI with
      consistent JSON shapes and proper HTTP status codes.
      `GET /v2/bookmarks` → `{bookmarks:[…sorted names…]}`.
      `POST /v2/bookmarks` body `{name}` → 201 / 409 on duplicate.
      `GET /v2/bookmarks/{name}` → `{name, count}`.
      `DELETE /v2/bookmarks/{name}` → 204.
      `PATCH /v2/bookmarks/{name}` body `{name}` → 200 (rename).
      `GET /v2/bookmarks/{name}/items` → `{name, items:[{sourceId,docId}…]}`.
      `PUT /v2/bookmarks/{name}/items` (add) / `DELETE /v2/bookmarks/{name}/items`
      (remove) both accept `[{sourceId,docId}…]` arrays.
      All mutating operations audited via `AuditLogger`. 404 returned for unknown
      bookmark names. Parallel v1 `Bookmarks.java` endpoint continues to work
      unchanged for backwards compat during the deprecation window.)
- [x] Jersey-vs-Spring consolidation decision recorded.
      (Decision: **keep Jersey as the engine-side API**. `iped-webapi` runs on embedded
      Jetty because it must start inside the engine JVM, co-loaded with Lucene, IItem,
      and the processing pipeline — Spring Boot's classpath management would conflict.
      `iped-webui-server` (Spring Boot) proxies to Jersey via a thin `RestTemplate`
      reverse-proxy. The two stacks are cleanly separated: Jersey owns forensic data
      access; Spring owns SSR page composition, auth, and island serving. No migration
      needed before broad v2 expansion.)

## Progress checks
- OpenAPI spec is the source of truth: CI fails on spec/implementation drift.
- Browser UI runs against real endpoints with zero stub controllers active.
