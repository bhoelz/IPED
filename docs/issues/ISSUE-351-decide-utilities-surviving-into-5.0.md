# ISSUE-351: Decide which utilities survive into 5.0

- Status: planned
- Roadmap: [iped-utils-ROADMAP.md](../roadmaps/iped-utils-ROADMAP.md)
- Roadmap section: Phase 3 — Long-term shape
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Decide what survives into IPED 5.0, treating utilities as a cost rather than an asset: prefer well-known libraries (commons-io, Guava-equivalents already on the classpath via Tika) over bespoke code where behavior is identical.

## Problem

Without a deliberate decision process, bespoke utility code tends to persist by default even when equivalent, better-maintained library functionality is already on the classpath, increasing long-term maintenance cost.

## Acceptance criteria

- [ ] Utilities with equivalent behavior in commons-io / Guava-equivalents (already on the classpath via Tika) identified.
- [ ] Decision recorded on which bespoke utilities are retired in favor of library equivalents for 5.0.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-utils-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
