# ISSUE-441: Lucene-backed additional-data storage and manager implementation

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 0 — Item-level additional task processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Concrete implementation of `IAdditionalDataSource` backed by a secondary Lucene index co-located with the case (`<output>/.additional-index/`), plus `DefaultAdditionalDataSourceManager` to register and query it.

## Problem

The API contracts (ISSUE-440) need a real, durable, upsert-capable store so re-running a task overwrites its own prior result without corrupting the main case index.

## Acceptance criteria

- [x] `LuceneAdditionalDataSource` implemented in `iped-additional-index`.
- [x] `DefaultAdditionalDataSourceManager` implemented in `iped-additional-index`.

## Updates

### 2026-06-21
- Issue created while triaging `docs/needs-revision/ADDITIONAL_PROCESSING.md` — verified `iped-engine-parent/iped-additional-index/src/main/java/iped/engine/additionalindex/{LuceneAdditionalDataSource,DefaultAdditionalDataSourceManager}.java` already exist, status set to `done`.
