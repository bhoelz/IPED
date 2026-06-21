# ISSUE-101: Typed config API consumed via iped-api contracts

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 2 — Configuration system completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Provide a typed config access API via `iped-api` contracts so tasks/parsers don't need to import registry internals directly.

## Problem

Tasks and parsers previously needed to reach into config registry internals to read typed configuration, creating an unwanted coupling to `iped-engine-core` internals.

## Acceptance criteria

- [x] `ITypedConfigAccess` defined in `iped-api`.
- [x] `ConfigurationManager` implements `ITypedConfigAccess`.
- [x] Tasks call `getConfig(MyConfig.class)` via the context instead of registry internals.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
