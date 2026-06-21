# ISSUE-047: TomlCarverConfiguration reads CarverConfig.toml via TomlMapper

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 1 — Hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Implement `TomlCarverConfiguration` in `iped-tasks-carving` to read `CarverConfig.toml` directly via `TomlMapper`. When no inline signatures are present for a carver entry, the named class is instantiated to supply its own `CarverType[]`.

## Problem

The carving task pipeline needed a configuration reader for the new TOML format, with a fallback path for carver entries that define their signatures in code rather than inline in the config.

## Acceptance criteria

- [x] `TomlCarverConfiguration` implemented in `iped-tasks-carving`, reading `CarverConfig.toml` via `TomlMapper`.
- [x] Fallback: when no inline sigs are present, the named class is instantiated to supply its `CarverType[]`.
- [x] `XMLCarverConfiguration` fields promoted to `protected` so the subclass can extend cleanly.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
