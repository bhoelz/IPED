# ISSUE-052: Pluggable validator hooks per carved type

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 3 — Capability growth
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a `CarvedItemValidator` interface to `iped-carvers-api` so third-party code can attach extra validation logic to any `CarverType` at startup without subclassing `AbstractCarver`, letting validators run after the built-in validation check.

## Problem

There was no extension point for additional, per-carved-type validation logic beyond the built-in checks baked into `AbstractCarver`, forcing anyone needing custom validation to subclass the carver itself.

## Acceptance criteria

- [x] `CarvedItemValidator` interface added to `iped-carvers-api`.
- [x] `CarverType.addValidator()` / `getValidators()` added.
- [x] `AbstractCarver.isValid()` runs all registered validators after the built-in `validateCarvedObject()` check.
- [x] Any validator returning `false` discards the candidate item.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
