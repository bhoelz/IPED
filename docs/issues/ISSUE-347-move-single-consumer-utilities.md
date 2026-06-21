# ISSUE-347: Move single-consumer utilities to their owning module

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 1 — Inventory and pruning
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Identify utilities in `iped.utils` that are actually used by only a single consumer module and move them into that module (e.g. utilities used only by parsers move to `iped-parsers-common`), reducing the blast radius of `iped-utils` changes.

## Problem

Utilities that belong conceptually to one consumer are currently centralized in the shared `iped-utils` module, making that module's high-blast-radius surface larger than necessary.

## Acceptance criteria

- [ ] Single-consumer utilities identified (depends on the usage inventory in ISSUE-344).
- [ ] Utilities used only by parsers moved to `iped-parsers-common` (and similarly for other single-consumer cases).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
