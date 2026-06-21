# ISSUE-385: Item tag endpoints (bookmark-backed, REST-ergonomic)

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 6 — Per-source ACL and item write endpoints
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add per-item tag endpoints, backed by the bookmark system, matching the semantics already exposed via the `iped_item_tag`/`iped_item_untag` MCP tools.

## Problem

Tagging an item via the REST API previously required working through the more generic bookmark batch endpoints rather than a direct, item-scoped tag resource.

## Acceptance criteria

- [x] `ItemTagsV2.java` at `v2/sources/{sourceId}/items/{id}/tags`: `GET` returns bookmark names for the item via `IIPEDSource.getBookmarks().getBookmarkList(id)`.
- [x] `PUT /…/tags/{tag}` auto-creates the bookmark on first use, then adds the item; audited.
- [x] `DELETE /…/tags/{tag}` removes the item from the bookmark; returns 204 when bookmark absent (idempotent).
- [x] Semantics match `iped_item_tag`/`iped_item_untag` MCP tool behavior exactly.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
