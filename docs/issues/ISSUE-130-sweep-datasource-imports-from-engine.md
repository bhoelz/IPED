# ISSUE-130: Sweep remaining datasource-specific code out of iped-engine

- Status: planned
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 1 — Complete the split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Search for remaining sleuthkit/ufed/ad1 imports inside `iped-engine` and invert them through `iped-api` SPIs.

## Problem

Even after extracting the dedicated datasource modules, `iped-engine` may still reference datasource-specific types directly. These references need to be found and inverted through SPI contracts so `iped-engine` has no compile-time dependency on any datasource module.

## Acceptance criteria

- [ ] Grep `iped-engine` for sleuthkit/ufed/ad1 imports.
- [ ] Replace each direct datasource import with an `iped-api` SPI call.
- [ ] Confirm zero remaining datasource-specific imports in `iped-engine`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `planned`.
