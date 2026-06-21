# ISSUE-029: Configuration schema contracts aligned with the TOML config system

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 3 — Modularization support
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add typed configuration access and validation hook contracts aligned with the TOML config system, without depending on the engine's registry implementation.

## Problem

Modules need a way to access and validate typed configuration without binding to the concrete engine registry implementation, to keep the API layer implementation-agnostic.

## Acceptance criteria

- [x] `iped.configuration.ITypedConfigAccess` added.
- [x] Implemented by `ConfigurationManager`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
