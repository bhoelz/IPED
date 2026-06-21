# ISSUE-175: Track/heatmap rendering parity for large coordinate sets

- Status: planned
- Roadmap: [iped-geo-ROADMAP.md](../roadmaps/iped-geo-ROADMAP.md)
- Roadmap section: Phase 2 — Web map experience (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Bring the browser map's track/heatmap rendering to parity with the Swing map for large coordinate sets, which requires server-side clustering for cases with more than 100k points.

## Problem

The Swing map currently handles large coordinate sets (tracks, heatmaps) better than the new browser map; without server-side clustering, the browser map degrades or becomes unusable above roughly 100k points.

## Acceptance criteria

- [ ] Server-side clustering implemented for coordinate sets larger than 100k points.
- [ ] Browser map track/heatmap rendering reaches parity with the Swing map for large cases.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-geo-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
