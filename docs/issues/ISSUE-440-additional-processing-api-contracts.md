# ISSUE-440: Additional-processing API contracts (IAdditionalDataSource, AdditionalItemData, manager)

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 0 — Item-level additional task processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Public contracts for storing and retrieving post-indexing task results per item: `IAdditionalDataSource`, `AdditionalItemData` DTO, and `IAdditionalDataSourceManager` for registering multiple sources.

## Problem

Re-running a task (OCR, NER, face recognition) on an already-indexed item needs somewhere to persist the new result without touching the read-only main Lucene index, and a contract-first design lets the storage implementation vary independently.

## Acceptance criteria

- [x] `IAdditionalDataSource` defined in `iped-api` (`iped.datasource`).
- [x] `AdditionalItemData` DTO defined in `iped-api`.
- [x] `IAdditionalDataSourceManager` defined in `iped-api`.

## Updates

### 2026-06-21
- Issue created while triaging `docs/needs-revision/ADDITIONAL_PROCESSING.md` — verified `iped-api/src/main/java/iped/datasource/{IAdditionalDataSource,AdditionalItemData,IAdditionalDataSourceManager}.java` already exist on disk, status set to `done`. This work was not previously reflected in the roadmap.
