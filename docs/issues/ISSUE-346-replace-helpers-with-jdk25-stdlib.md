# ISSUE-346: Replace hand-rolled helpers with JDK 25 standard library equivalents

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 1 — Inventory and pruning
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Replace hand-rolled helpers in `iped.utils` (e.g. file/stream/string operations) that now have equivalents in the JDK 25 standard library, and deprecate the local versions.

## Problem

Many utilities in this module were written before the JDK gained native equivalents; keeping bespoke implementations around once the standard library covers the same behavior adds unnecessary maintenance burden.

## Acceptance criteria

- [ ] Hand-rolled file/stream/string helpers with JDK 25 stdlib equivalents identified.
- [ ] Local versions deprecated in favor of the stdlib equivalents.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
