# ISSUE-046: CarverConfig.toml — full TOML translation of CarverConfig.xml

- Status: done
- Roadmap: [iped-carvers-ROADMAP.md](../roadmaps/iped-carvers-ROADMAP.md)
- Roadmap section: Phase 1 — Hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Create `CarverConfig.toml` in `iped-carvers-impl/src/main/resources/iped/config/defaults/conf/` as the full TOML translation of `CarverConfig.xml`, covering all 28 active carver types with their signatures, size bounds, length-ref offsets, and MIME types, in line with the project's TOML config migration.

## Problem

Carver configuration was still defined in the legacy XML format, inconsistent with the rest of the project's move to module-owned TOML defaults.

## Acceptance criteria

- [x] `CarverConfig.toml` created under `iped-carvers-impl/src/main/resources/iped/config/defaults/conf/`.
- [x] All 28 active carver types covered, including signatures, size bounds, length-ref offsets, and MIME types.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-carvers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
