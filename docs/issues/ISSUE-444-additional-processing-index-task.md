# ISSUE-444: AdditionalIndexTask as the terminal task of the re-processing pipeline

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 0 — Item-level additional task processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`AdditionalIndexTask` is the terminal step of the additional-processing pipeline, persisting a re-run task's output to the additional data source instead of the main Lucene index.

## Problem

The minimal re-processing pipeline (ISSUE-443) needs a terminal task analogous to the main pipeline's `IndexTask`, but writing to the additional store.

## Acceptance criteria

- [x] `AdditionalIndexTask` implemented and used as the last stage of the additional-task pipeline.

## Updates

### 2026-06-21
- Issue created while triaging `docs/needs-revision/ADDITIONAL_PROCESSING.md` — verified `iped-engine-parent/iped-engine/src/main/java/iped/engine/task/additional/AdditionalIndexTask.java` already exists, status set to `done`.
