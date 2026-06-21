# ISSUE-045: ArchUnit test enforcing carver module boundaries

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 1 — Hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add an ArchUnit test in `iped-carvers-impl` enforcing that no carver class imports `iped.engine.*` or `iped.app.*`, keeping carvers decoupled from the engine and desktop app modules.

## Problem

Without an automated boundary check, carver implementations could accidentally take on a dependency on engine or app internals, undermining the carvers module's independence.

## Acceptance criteria

- [x] `CarversBoundaryTest` added in `iped-carvers-impl`.
- [x] Test fails if a carver class imports `iped.engine.*` or `iped.app.*`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
