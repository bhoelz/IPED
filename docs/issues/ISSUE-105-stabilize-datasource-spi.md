# ISSUE-105: Stabilize the datasource SPI for external/closed-source plugins

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Stabilize the datasource SPI in `iped-engine-core` so external or closed-source readers can be packaged as plugins without forking the engine.

## Problem

Third-party datasource readers need a stable, documented SPI contract to integrate without requiring a fork of the engine codebase.

## Acceptance criteria

- [x] `iped.datasource.spi.IDataSourceReader` established as the stable SPI in `iped-api`.
- [x] `iped-engine-core` discovery via `ServiceLoader<IDataSourceReader>` established as the wiring contract.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
