# ISSUE-026: Introduce stable surface for the JS/Python scripting SDKs

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 2 — Versioned API for IPED 5.0
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Introduce the stable API surface needed by the JS/Python scripting SDKs (workstream 6 of the root `ROADMAP.md`): case/query/result navigation, item content/metadata access, tagging/bookmarks, and export/job orchestration.

## Problem

The scripting SDKs need a dedicated, stable set of interfaces rather than reusing internal engine types, so that scripting integrations remain stable across engine refactors.

## Acceptance criteria

- [x] `iped.scripting.ICaseNavigator` added.
- [x] `iped.scripting.IItemAccessor` added.
- [x] `iped.scripting.ITaggingService` added.
- [x] `iped.scripting.IBookmarkService` added.
- [x] `iped.scripting.IExportService` added.
- [x] `iped.scripting.IJobOrchestrator` added.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
