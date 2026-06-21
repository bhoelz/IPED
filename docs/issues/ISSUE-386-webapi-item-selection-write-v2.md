# ISSUE-386: Item selection write v2 endpoint

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 6 — Per-source ACL and item write endpoints
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a per-item selection (checked/unchecked) REST endpoint to complement the existing v1 batch `/selection` endpoints, for item-by-item selection workflows.

## Problem

The browser UI's item-level selection workflows needed a per-item REST resource rather than only the batch v1 selection endpoint.

## Acceptance criteria

- [x] `ItemSelectionV2.java` at `v2/sources/{sourceId}/items/{id}/selected`: `GET` returns `{selected: bool}` via `IIPEDSource.getBookmarks().isChecked(id)`.
- [x] `PUT` marks the item as checked via `SelectionService.add()`.
- [x] `DELETE` unchecks the item via `SelectionService.remove()`.
- [x] All mutations audited.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
