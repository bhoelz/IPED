# ISSUE-021: Audit all interfaces for engine-implementation leakage

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 1 — Contract hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Audit all `iped-api` interfaces for types that force a concrete engine class into method signatures, replacing any such leakage with API-level abstractions. This keeps the public contract layer free of engine implementation details.

## Problem

Interfaces in `iped-api` could accidentally leak engine-specific types (e.g. concrete Lucene/Tika classes) into their signatures, coupling consumers to implementation details instead of the abstract contract.

## Acceptance criteria

- [x] All method signatures reviewed for engine-implementation leakage.
- [x] Lucene/Tika-derived return types use `Object` return types rather than concrete engine types.
- [x] Only iped-api interface types appear in parameters; no engine leakage found.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
