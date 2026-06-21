# ISSUE-305: Add IndexSettings interface and findObjectInstanceOf lookup to break the IndexTaskConfig cycle

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 1 — Finish config/code ownership moves
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

To move `IndexTaskConfig` out of `iped-engine` without creating a module dependency
cycle, an `IndexSettings` interface was added to `iped-engine-core` along with a new
`instanceof`-based configuration lookup, `ConfigurationManager.findObjectInstanceOf`.

## Problem

`Manager`, `AppAnalyzer`, `ConfiguredFSDirectory`, and `QueryBuilder` (all in
`iped-engine`) couldn't depend on the concrete `IndexTaskConfig` class without
creating a cycle, since `iped-tasks-storage-index` already depends on `iped-engine`.
The existing `ConfigurationManager.findObject(Class)` lookup matches by exact class
equality, so an interface alone wasn't enough — the engine side needed a concrete
class to pass as the lookup key.

## Acceptance criteria

- [x] `IndexSettings` interface added to `iped-engine-core` exposing the 7 accessors
      the four engine classes actually call: `isUseNIOFSDirectory`, `isForceMerge`,
      `getCommitIntervalSeconds`, `getMaxTokenLength`, `isFilterNonLatinChars`,
      `isConvertCharsToAscii`, `isConvertCharsToLowerCase`, `getExtraCharsToIndex`.
- [x] `ConfigurationManager.findObjectInstanceOf(Class<I>)` added as an
      `instanceof`-based lookup alongside the existing exact-class `findObject`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
