# ISSUE-222: Extend the no-engine-imports boundary rule to every parser module

- Status: in_progress
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 3 — Architecture
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Parsers should consume only `iped-api` + Tika + `iped-parsers-common`, with no engine
imports, enforced by an ArchUnit rule applied to every child module — not just the
16 modules `iped-parsers-impl` happens to depend on.

## Problem

Previously only `iped-parsers-impl`'s `ParsersBoundaryTest` enforced the no-engine-
import rule, and only for the 16 modules it transitively depends on (`apk`,
`browsers`, `compression`, `database-edb`, `db-base`, `discord-cache`,
`mp4-detector`, `plist-detector`, `sqlite-core`, `telegram`, `threema`, `tor`,
`ufed`, `vcard`, `video`, `whatsapp`). 16 other modules (`ares`, `bittorrent`,
`eventtranscript`, `external`, `gdrive`, `emule`, `lnk`, `mail`, `registry`,
`security`, `shareaza`, `skype`, `sqlite-detector`, `usnjrnl`, `vlc`, `winx`) had
zero boundary enforcement.

## Acceptance criteria

- [x] Add a dedicated `XxxBoundaryTest` (same two-rule shape as
      `ParsersBoundaryTest`) to each of the 16 previously-uncovered modules' own
      `src/test/java/iped/arch/`, scanning only that module's own package (32 tests
      total, all green).
- [ ] Build a true parent-level rule (one test that runs against every child
      module's output) to replace the transitive-only coverage currently relied on
      for the 16 modules covered indirectly via `iped-parsers-impl` — needs a
      multi-module ArchUnit test setup not yet built.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `in_progress` based on the original `[~]` marker.
