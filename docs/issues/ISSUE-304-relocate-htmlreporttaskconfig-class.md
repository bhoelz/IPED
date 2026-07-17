# ISSUE-304: Relocate the HtmlReportTaskConfig class itself to iped-tasks-report

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 1 — Finish config/code ownership moves
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-07-17

## Summary

Only `HTMLReportConfig.toml` was moved to `iped-tasks-report` so far; the
`HtmlReportTaskConfig` `.java` class itself is still owned by `iped-engine`, with
`HTMLReportTask` in `iped-tasks-report` resolving it via a wildcard import through
its existing `iped-engine` dependency.

## Problem

The earlier roadmap entry claiming this move was complete was inaccurate — it was a
stale completion claim covering only the TOML file, not the Java class. This is the
same root blocker as `IndexTaskConfig` (ISSUE-306): no consumer inside `iped-engine`
itself reads the config directly, so once the `IndexSettings`-interface pattern is
proven out, this becomes a pure move with no engine-internal callers to redirect.

## Acceptance criteria

- [x] Move `HtmlReportTaskConfig.java` out of `iped-engine` into `iped-tasks-report`.
- [x] Verify no split-package hazard exists (confirm only one copy of the class).
- [x] Update `HTMLReportTask`'s wildcard-import resolution accordingly.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `in_progress` based on the original `[~]` marker.

### 2026-07-17
- Confirmed the class is owned by `iped-tasks/iped-tasks-report` and no copy remains under `iped-engine`.
- `HTMLReportTask` resolves the moved configuration through its existing `iped.engine.config.*` import.
- Verified with `mvn --% -pl iped-tasks/iped-tasks-report -am -DskipTests compile` (BUILD SUCCESS).
