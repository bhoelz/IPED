# ISSUE-003: AD1 test fixtures and decode regression tests

- Status: planned
- Roadmap: [iped-ad1-ROADMAP.md](../roadmaps/iped-ad1-ROADMAP.md)
- Roadmap section: Phase 2 — Robustness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The module needs a set of small AD1 sample files — single-segment, multi-segment (`.ad1/.ad2...`), compressed entries, and corrupted tail — paired with decode regression tests to protect against future regressions.

## Problem

There is no fixture-backed regression suite covering the variety of AD1 layouts the decoder must handle, so changes to the decoder risk silently breaking support for less common cases (multi-segment, compressed, or corrupted images).

## Acceptance criteria

- [ ] Fixture set includes single-segment, multi-segment, compressed-entry, and corrupted-tail AD1 samples.
- [ ] Each fixture has an associated decode regression test.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ad1-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
