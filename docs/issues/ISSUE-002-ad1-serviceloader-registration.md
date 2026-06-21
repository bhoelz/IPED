# ISSUE-002: Register ad1 datasource via ServiceLoader SPI

- Status: planned
- Roadmap: [iped-ad1-ROADMAP.md](../roadmaps/iped-ad1-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Once a `ServiceLoader`-based datasource SPI is available, the `iped-ad1` reader should register itself through that mechanism instead of relying on hardcoded engine wiring, reducing coupling between the engine and the datasource module.

## Problem

The AD1 datasource reader is currently wired into the engine directly rather than discovered dynamically, which keeps the engine aware of the module's existence even though the long-term goal is full decoupling.

## Acceptance criteria

- [ ] AD1 reader registers itself through the `ServiceLoader` datasource SPI once that SPI lands in `iped-api`.
- [ ] No hardcoded engine-side reference to the AD1 reader remains after migration.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ad1-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
