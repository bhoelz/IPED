# ISSUE-216: Add ParsersBoundaryTest ArchUnit guard to iped-parsers-impl

- Status: done
- Roadmap: [iped-parsers-ROADMAP.md](../roadmaps/iped-parsers-ROADMAP.md)
- Roadmap section: Phase 1 — Finish the per-parser split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

An ArchUnit guard now enforces that no parser class in `iped-parsers-impl` imports
engine or app internals, locking in the architectural boundary established by the
per-parser split work.

## Problem

Without an automated check, nothing prevented parser code from re-acquiring a
dependency on `iped.engine.*`/`iped.app.*` after the split.

## Acceptance criteria

- [x] `ParsersBoundaryTest` added to `iped-parsers-impl`.
- [x] Test enforces no parser class imports `iped.engine.*` or `iped.app.*`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-parsers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
