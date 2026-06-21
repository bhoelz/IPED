# ISSUE-443: Additional-task runner and worker for re-running tasks on selected items

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 0 — Item-level additional task processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`AdditionalTaskWorker`/`AdditionalTaskRunner`/`AdditionalTaskProgress` execute an existing task against a selected set of already-indexed items and persist results via the additional data source, without ever holding a reference to the main case's `IndexWriter`.

## Problem

Re-processing selected items needs its own minimal pipeline runner — reusing the full `Manager`/`Worker` machinery would risk accidental writes to the main index.

## Acceptance criteria

- [x] `AdditionalTaskWorker` implemented — no `IndexWriter` access, fails fast on tasks that try to create child items.
- [x] `AdditionalTaskRunner` implemented — thread pool, per-item progress callback, cancellation support.
- [x] `AdditionalTaskProgress` implemented.
- [x] `@AdditionalProcessingCapable` annotation (in `iped-api` `iped.task`) marks tasks eligible for re-execution.

## Updates

### 2026-06-21
- Issue created while triaging `docs/needs-revision/ADDITIONAL_PROCESSING.md` — verified `iped-engine-parent/iped-engine/src/main/java/iped/engine/task/additional/{AdditionalTaskWorker,AdditionalTaskRunner,AdditionalTaskProgress}.java` and the `AdditionalProcessingCapable` annotation already exist, status set to `done`.
