# ISSUE-382: Bookmark CRUD v2 REST layer

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 5 — Bookmark CRUD and stack consolidation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Build a v2 REST layer over the existing `BookmarkService` SPI with consistent JSON shapes and proper HTTP status codes (EPIC-WEB-08 / WEB-071), while keeping the v1 bookmark endpoint working for backwards compatibility.

## Problem

The v1 bookmark endpoint predated the v2 conventions (JSON shape, status codes, audit logging) and needed a v2-consistent counterpart for the browser UI and MCP tooling.

## Acceptance criteria

- [x] `BookmarksV2.java` implements `GET /v2/bookmarks`, `POST /v2/bookmarks` (201/409 on duplicate), `GET /v2/bookmarks/{name}`, `DELETE /v2/bookmarks/{name}` (204), `PATCH /v2/bookmarks/{name}` (rename), `GET /v2/bookmarks/{name}/items`, `PUT`/`DELETE /v2/bookmarks/{name}/items`.
- [x] All mutating operations audited via `AuditLogger`.
- [x] 404 returned for unknown bookmark names.
- [x] Parallel v1 `Bookmarks.java` endpoint continues to work unchanged during the deprecation window.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
