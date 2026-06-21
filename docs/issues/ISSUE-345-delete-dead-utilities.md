# ISSUE-345: Delete dead utilities with zero usages across the reactor

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 1 — Inventory and pruning
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Delete utility classes/methods in `iped.utils` that have zero usages across the reactor, following the project's established dependency-cleanup convention of removing rather than keeping code "just in case".

## Problem

A grab-bag utility module accumulates dead code over time; unused utilities add maintenance and audit surface with no benefit and should be removed once the usage inventory (ISSUE-344) confirms zero usages.

## Acceptance criteria

- [ ] Utilities with zero usages across the reactor identified (depends on the inventory in ISSUE-344).
- [ ] Dead utilities deleted rather than retained speculatively.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
