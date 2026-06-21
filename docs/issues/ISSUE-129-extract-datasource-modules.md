# ISSUE-129: Extract datasource modules iped-sleuthkit, iped-ufed, iped-ad1

- Status: done
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 1 — Complete the split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Extract `iped-sleuthkit`, `iped-ufed`, and `iped-ad1` as standalone datasource-reader submodules.

## Problem

Datasource-specific reader code (Sleuthkit disk images, UFED/Cellebrite XML, AD1) was bundled into the engine monolith; splitting it into focused, independently buildable modules is required for the target architecture where datasource modules depend only on `iped-engine-core`.

## Acceptance criteria

- [x] `iped-sleuthkit` extracted as a standalone submodule.
- [x] `iped-ufed` extracted as a standalone submodule.
- [x] `iped-ad1` extracted as a standalone submodule.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `done`.
