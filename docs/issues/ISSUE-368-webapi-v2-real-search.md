# ISSUE-368: Implement real v2 search against the engine

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 1 — v2 contract completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Replace the temporary `SearchStubController` with a real v2 search endpoint backed by the engine's Lucene index, and contract-test it against the OpenAPI spec in CI.

## Problem

The browser UI was leaning on a stub search controller instead of real engine search, blocking real triage workflows and risking drift from the OpenAPI contract.

## Acceptance criteria

- [x] `GET /v2/search?q=...&offset=0&limit=50` implemented in `SearchV2.java`.
- [x] Backed by `SearchService.searchPaginated()` SPI (iped-engine-core) and `EngineSearchService` running real Lucene search.
- [x] Returns `SearchResultPageJSON` with `{total, offset, limit, items[]}`, each item carrying name/path/mediaType/size/hash/dates/categories.
- [x] Contract-tested against the OpenAPI spec in CI.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
