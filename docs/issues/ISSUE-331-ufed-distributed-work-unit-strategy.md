# ISSUE-331: Decide UFED distributed work-unit strategy

- Status: planned
- Roadmap: [iped-ufed-ROADMAP.md](../roadmaps/iped-ufed-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed processing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Because UFDR sections are sequential XML, a decision is needed on the distributed work-unit story: either keep UFED evidence processing on a single agent, or implement a split-by-section pre-pass; the chosen approach must be documented.

## Problem

UFED's sequential XML structure does not naturally support the same segment-addressable splitting used by other datasources, so distributed processing support for UFED is undefined.

## Acceptance criteria

- [ ] A decision is made and documented: single-agent UFED processing vs split-by-section pre-pass.
- [ ] If split-by-section is chosen, the pre-pass mechanism is specified.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ufed-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
