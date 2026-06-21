# ISSUE-294: Corrupt-image handling test corpus for Sleuthkit reader

- Status: planned
- Roadmap: [iped-sleuthkit-ROADMAP.md](../roadmaps/iped-sleuthkit-ROADMAP.md)
- Roadmap section: Phase 2 — Robustness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A test corpus covering truncated E01 images, bad partition tables, and exotic file systems is needed to confirm the reader degrades per-entry rather than aborting the entire evidence item.

## Problem

There is no fixture-backed corpus validating the reader's resilience against corrupted or unusual disk image inputs.

## Acceptance criteria

- [ ] Test corpus includes truncated E01, bad partition table, and exotic filesystem samples.
- [ ] Reader degrades per-entry (skips/reports affected items) instead of aborting the whole evidence on any of these inputs.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-sleuthkit-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
