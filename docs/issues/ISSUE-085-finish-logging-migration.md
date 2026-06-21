# ISSUE-085: Finish logging migration to @Slf4j/log4j2 in remaining classes

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 1 — Slim down to orchestration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Complete the migration of remaining `System.out`/`System.err`/`java.util.logging` usages to `@Slf4j`/log4j2, excluding the documented lazy-init logger exceptions.

## Problem

Several classes (`IPEDCrawler`, `CmdLineArgsImpl`, `ProcessUtil`, `CustomIndexDeletionPolicy`, `IPEDReader`) still used raw `System.out`/`System.err` printing or stray `java.util.logging` imports instead of the project's standard logging convention.

## Acceptance criteria

- [x] `IPEDCrawler`: 12+ `System.out/err.println` converted to `log.info/error`.
- [x] `CmdLineArgsImpl`: 2 `System.out.println` converted to `log.info/error`.
- [x] `ProcessUtil`: `System.err.println` + `printStackTrace` converted to `log.error(..., e)`.
- [x] `CustomIndexDeletionPolicy`: 2 `System.out.println` converted to `log.info`.
- [x] Unused `java.util.logging` imports removed from `IPEDReader`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `done`.
