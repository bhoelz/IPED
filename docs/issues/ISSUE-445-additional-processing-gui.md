# ISSUE-445: GUI for selecting items and re-running additional processing

- Status: done
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 0 — Item-level additional task processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Expose the already-implemented additional-processing backend (ISSUE-440 through ISSUE-444) to analysts: a context-menu action, a task-selection dialog, a progress panel, and a results-table indicator/filter for which items have additional data.

## Problem

The backend for re-running tasks on selected items and storing results is fully built, but there is no UI entry point — the feature is currently unreachable from the desktop app.

## Acceptance criteria

- [x] "Process selected" context-menu/toolbar action, enabled only when items are selected and an `@AdditionalProcessingCapable` task is available.
- [x] `AdditionalProcessingDialog` listing eligible tasks with a thread-count and skip-already-processed option.
- [x] `AdditionalTaskProgressPanel` with per-item progress, error log, and cancel support.
- [x] Results-table indicator/filter integration through the additional-result filterer.
- [x] Filter by "has additional task result".

## Updates

### 2026-07-16
- Implemented the Swing dialog, task discovery, thread/skip controls,
  progress/cancel panel, and selected-items context-menu action.

### 2026-06-21
- Issue created while triaging `docs/needs-revision/ADDITIONAL_PROCESSING.md` — this is the one part of the original plan (its Phase 5) with no corresponding code found anywhere in the repo; status set to `planned`.
