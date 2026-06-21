# ISSUE-023: Javadoc coverage for every public type in iped-api

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 1 — Contract hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Ensure every public type in `iped-api` has complete Javadoc, since this module is the documentation surface for the 5.0 scripting APIs.

## Problem

Scripting API consumers rely on `iped-api` Javadoc as their primary documentation source; gaps in coverage directly hurt the scripting SDK's usability.

## Acceptance criteria

- [x] `IDataSource` fully documented.
- [x] `IIPEDSource` fully documented.
- [x] `IIPEDSearcher` fully documented.
- [x] `IItemSearcher` fully documented.
- [x] `IMultiSearchResult` fully documented.
- [x] `IItemId` fully documented.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
