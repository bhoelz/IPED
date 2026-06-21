# ISSUE-128: Extract iped-engine-core, iped-engine-graph, iped-engine-hashdb

- Status: done
- Roadmap: [iped-engine-parent-ROADMAP.md](../roadmaps/iped-engine-parent-ROADMAP.md)
- Roadmap section: Phase 1 — Complete the split
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Extract `iped-engine-core`, `iped-engine-graph`, and `iped-engine-hashdb` as standalone submodules of the engine split.

## Problem

The historical monolithic `iped-engine` bundled shared abstractions, graph analysis, and hash database lookups together; splitting them into focused submodules with enforced dependency boundaries is the foundation of the broader engine-split effort.

## Acceptance criteria

- [x] `iped-engine-core` extracted as a standalone submodule.
- [x] `iped-engine-graph` extracted as a standalone submodule.
- [x] `iped-engine-hashdb` extracted as a standalone submodule.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-parent-ROADMAP.md`, status set to `done`.
