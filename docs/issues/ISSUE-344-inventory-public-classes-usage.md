# ISSUE-344: Inventory all public classes and map usage per consumer module

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 1 — Inventory and pruning
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Build an inventory of all public classes in `iped.utils` and map their actual usage per consumer module across the reactor, as the foundation for the pruning and restructuring work planned for this module.

## Problem

`iped-utils` is a single flat package accumulated over years with no current record of which classes are actually used by which consumer modules, making it impossible to safely prune or restructure without first establishing a usage baseline.

## Acceptance criteria

- [ ] All public classes in `iped.utils` enumerated.
- [ ] Usage of each class mapped per consumer module across the full reactor.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
