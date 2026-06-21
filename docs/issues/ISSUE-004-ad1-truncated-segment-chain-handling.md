# ISSUE-004: Graceful handling of truncated AD1 segment chains

- Status: planned
- Roadmap: [iped-ad1-ROADMAP.md](../roadmaps/iped-ad1-ROADMAP.md)
- Roadmap section: Phase 2 — Robustness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

When an AD1 multi-segment chain is missing trailing segments, the reader should report the missing segments per affected item rather than failing the entire evidence item, improving resilience against incomplete acquisitions.

## Problem

Currently a truncated segment chain likely causes the whole evidence item to fail processing instead of degrading gracefully on a per-item basis.

## Acceptance criteria

- [ ] Missing/truncated segments are detected and reported per item.
- [ ] Evidence processing continues for items unaffected by the truncation instead of aborting the whole evidence.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ad1-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
