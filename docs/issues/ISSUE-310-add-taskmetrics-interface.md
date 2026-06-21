# ISSUE-310: Add TaskMetrics interface to iped-tasks.spi

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 2 — SPI maturity
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A `TaskMetrics` interface was added to `iped-tasks.spi`; tasks implementing it
expose `processedCount`, `errorCount`, `processingMillis`, and `itemsPerSecond()`,
discovered by the engine via `instanceof` on the task object.

## Problem

There was no standard way for tasks to expose processing performance metrics to
the engine for monitoring/reporting purposes.

## Acceptance criteria

- [x] `TaskMetrics` interface added to `iped-tasks.spi` exposing `processedCount`,
      `errorCount`, `processingMillis`, `itemsPerSecond()`.
- [x] Engine discovers `TaskMetrics` implementations via `instanceof` on the task
      object.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
