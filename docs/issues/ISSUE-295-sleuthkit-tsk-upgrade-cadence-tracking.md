# ISSUE-295: Track TSK upgrade cadence and document carried patch set

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 2 — Robustness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Upstream Sleuthkit releases should be tracked, and the patch set carried in the currently pinned `4.12.0.p1` version should be documented so future upgrades don't silently drop fixes IPED depends on.

## Problem

The patch set applied on top of upstream Sleuthkit `4.12.0` is not documented, risking silent loss of fixes when upgrading to a newer TSK version.

## Acceptance criteria

- [ ] The patches carried in `4.12.0.p1` are documented.
- [ ] A process exists for tracking upstream Sleuthkit releases against the pinned version.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
