# ISSUE-005: Document AD1 format knowledge in FORMAT.md

- Status: planned
- Roadmap: [iped-ad1-ROADMAP.md](../roadmaps/iped-ad1-ROADMAP.md)
- Roadmap section: Phase 2 — Robustness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The AD1 format knowledge currently embedded in the decoder — header layout, chunk compression scheme — should be written up in a `FORMAT.md` document so it is not only tribal knowledge held by whoever last touched the decoder.

## Problem

Format details needed to maintain or extend the decoder live only in code, making onboarding and future maintenance harder than necessary.

## Acceptance criteria

- [ ] A `FORMAT.md` document exists describing AD1 header layout and chunk compression.
- [ ] Document is kept alongside the decoder source so it stays discoverable.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ad1-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
