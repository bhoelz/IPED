# ISSUE-132: Replace hardcoded datasource wiring with ServiceLoader-discovered readers

- Status: planned
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 2 — Service-loader datasource registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Replace hardcoded datasource wiring in `iped-engine` with `ServiceLoader`-discovered readers registered against the `iped-api` SPI.

## Problem

`iped-engine` currently wires datasource readers directly rather than discovering them dynamically, which keeps it coupled to specific datasource implementations and blocks the goal of making datasource modules optional at runtime.

## Acceptance criteria

- [ ] Identify all hardcoded datasource reader references in `iped-engine`.
- [ ] Replace each with `ServiceLoader<IDataSourceReader>` discovery.
- [ ] Verify engine builds and runs correctly with readers wired only via SPI.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `planned`.
