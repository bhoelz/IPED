# ISSUE-419: Wire info panel, viewer fragments, and sidebar trees to real endpoints

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 1 — Real data end-to-end
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The sidebar "Categories" tab calls `GET /v2/sources/{caseId}/items/categories`, "Bookmarks" calls the v1 `/bookmarks` endpoint, and the viewer "meta" tab plus info panel call `GET /v2/sources/{src}/items/{id}` (ItemMetadataJSON), flattening structured fields and Tika metadata for display.

## Problem

These fragments previously rendered static or stub HTML rather than live case data.

## Acceptance criteria

- [x] Sidebar "Categories" calls the real categories endpoint.
- [x] Sidebar "Bookmarks" calls the real bookmarks endpoint.
- [x] Viewer meta tab and info panel call the real item-metadata endpoint and flatten its fields for display.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
