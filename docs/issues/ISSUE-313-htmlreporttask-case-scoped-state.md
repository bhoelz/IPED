# ISSUE-313: Move HtmlReportTask static state into a per-case ReportAccumulator

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`HtmlReportTask`'s static state (`entriesByLabel/Category/NoLabel` collections,
never cleared between cases per the classification matrix in ISSUE-312) was moved
into a `ReportAccumulator` stored in `caseData`, making the task safe to run
across multiple cases in a batch.

## Problem

`HtmlReportTask` held static collections that were never cleared between cases,
risking cross-case data bleed in batch or report runs.

## Acceptance criteria

- [x] `entriesByLabel`, `entriesByCategory`, `entriesNoLabel`,
      `imageThumbsByLabel`, `currentFiles`, `reportSubFolder`,
      `externalImageConverter`, and `init` (AtomicBoolean) moved into a
      per-case `ReportAccumulator` stored on `caseData`.
- [x] Double-checked locking via `synchronized(HTMLReportTask.class)` ensures
      exactly one `ReportAccumulator` per `caseData` instance.
- [x] `getReportSubFolder()` changed to an instance method.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
