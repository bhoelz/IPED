# ISSUE-176: Decommission the embedded JavaFX/WebKit map path

- Status: planned
- Roadmap: [iped-geo-ROADMAP.md](../roadmaps/iped-geo-ROADMAP.md)
- Roadmap section: Phase 3 — Decommission embedded WebKit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Once browser-map parity is signed off, retire the JavaFX/WebKit map path along with the rest of the Swing UI, while keeping KML export available as a standalone data feature.

## Problem

The embedded JavaFX/WebKit map is redundant maintenance burden once the browser map reaches parity; it should be removed as part of the broader Swing UI decommission, but only after parity is confirmed.

## Acceptance criteria

- [ ] Browser-map parity signed off (depends on ISSUE-175).
- [ ] JavaFX/WebKit map path retired alongside the rest of the Swing UI.
- [ ] KML export retained as a data feature independent of the rendering path.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-geo-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
