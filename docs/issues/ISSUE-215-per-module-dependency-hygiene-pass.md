# ISSUE-215: Per-module dependency hygiene pass across split parser modules

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A hygiene pass removed legacy dependencies and fixed dependency-scope/edge issues
across the parser modules touched by the recent split work.

## Problem

Several split-out and existing modules carried legacy `commons-lang` 2.6 usage,
overly broad `sqlite-jdbc` compile-scope dependencies, and an unnecessary backwards
dependency edge from `iped-parser-skype` onto `iped-parsers-impl`.

## Acceptance criteria

- [x] Legacy `commons-lang` 2.6 removed from `iped-parser-registry`,
      `iped-parser-skype`, `iped-parser-whatsapp`, `iped-parsers-impl`; affected
      `ArrayUtils`/`StringUtils` imports migrated to `commons-lang3`.
- [x] `sqlite-jdbc` moved to `runtime` scope in `iped-parser-eventtranscript`,
      `iped-parser-gdrive`, `iped-parser-winx` (no compile-time `org.sqlite.*`
      import); left at compile scope in `iped-parser-sqlite-core`/
      `iped-parser-sqlite-detector` and `iped-parsers-impl` (`OCRParser`) where
      `org.sqlite.SQLiteConfig` is referenced directly.
- [x] `iped-parser-skype` no longer depends on `iped-parsers-impl`; its
      `ReportGenerator` now uses a local `iped.parsers.skype.Util` replicating
      `getItems`/`getExportPath`, removing the backwards dependency edge.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
