# ISSUE-211: Split iped-parser-threema out of iped-parsers-impl

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Threema parsing (10+ classes) was split out of `iped-parsers-impl` into its own
Maven module as part of the per-parser modularization effort.

## Problem

Threema parser code lived inside the monolithic `iped-parsers-impl` aggregate instead
of its own Maven module.

## Acceptance criteria

- [x] `iped-parser-threema` module created and compiles clean.
- [x] 9 parser classes + CSS/JS/img resources moved.
- [x] `META-INF/services` wired.
- [x] Report-path helpers (`getExportPath`, `getReportHref`, `getSourceFileIfExists`,
      `getItems`) added to a local `Util` class.
- [x] `iped-parser-db-base` (ItemInfo), `iped-parser-plist-detector`, dd-plist, guava,
      jackson deps declared in the new module's pom.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
