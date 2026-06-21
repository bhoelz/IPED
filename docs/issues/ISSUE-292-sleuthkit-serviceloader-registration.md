# ISSUE-292: Register Sleuthkit reader via ServiceLoader SPI

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Once the datasource SPI lands in `iped-api`, the Sleuthkit reader should register itself through `ServiceLoader` discovery instead of relying on hardcoded engine wiring.

## Problem

The Sleuthkit reader is currently wired into the engine directly, keeping the engine coupled to its existence rather than discovering it dynamically.

## Acceptance criteria

- [ ] Sleuthkit reader registers via the `ServiceLoader` datasource SPI once available in `iped-api`.
- [ ] No hardcoded engine-side reference to the Sleuthkit reader remains after migration.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
