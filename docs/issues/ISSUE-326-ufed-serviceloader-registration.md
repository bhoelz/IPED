# ISSUE-326: Register UFED reader via ServiceLoader SPI

- Status: planned
- Roadmap: [iped-ufed-ROADMAP.md](../roadmaps/iped-ufed-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Once the datasource SPI is available, the UFED/Cellebrite reader should register itself through `ServiceLoader` discovery rather than relying on hardcoded engine wiring.

## Problem

The UFED reader is currently wired into the engine directly, keeping the engine coupled to its existence.

## Acceptance criteria

- [ ] UFED reader registers via the `ServiceLoader` datasource SPI once available.
- [ ] No hardcoded engine-side reference to the UFED reader remains after migration.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ufed-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
