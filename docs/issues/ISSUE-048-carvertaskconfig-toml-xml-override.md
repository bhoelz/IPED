# ISSUE-048: Wire CarverTaskConfig to load TOML first with XML overrides

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 1 — Hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Wire `CarverTaskConfig` so the TOML file is loaded first as the primary configuration source, with user-supplied `CarverConfig.xml` / `carver-*.xml` files applied on top as overrides, preserving backward compatibility with existing case-level XML configuration.

## Problem

Migrating the default config to TOML must not break existing cases that rely on case-level XML carver configuration overrides.

## Acceptance criteria

- [x] `CarverTaskConfig` loads the TOML file first as primary source.
- [x] User-supplied `CarverConfig.xml` / `carver-*.xml` are applied on top as overrides.
- [x] Backward compatible with existing case-level XML configuration.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
