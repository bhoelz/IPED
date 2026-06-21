# ISSUE-369: Item retrieval, content preview, metadata/facets, category/bookmark trees

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 1 — v2 contract completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add the v2 endpoints the browser UI's priority workflows need: item retrieval with full metadata, and category listing.

## Problem

The browser UI needed endpoints to fetch a single item's full metadata and the case's category tree, which did not yet exist under the v2 contract.

## Acceptance criteria

- [x] `GET /v2/sources/{sourceId}/items/{id}` implemented in `ItemsV2.java`, returning `ItemMetadataJSON` with all IItem fields plus full Tika metadata map, bookmarks, and selection state.
- [x] `GET /v2/sources/{sourceId}/items/categories` returns the sorted leaf-category list via `IIPEDSource.getLeafCategories()`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
