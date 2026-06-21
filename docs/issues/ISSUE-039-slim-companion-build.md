# ISSUE-039: Slim companion build with native-dependent viewers only

- Status: planned
- Roadmap: [iped-app-ROADMAP.md](../roadmaps/iped-app-ROADMAP.md)
- Roadmap section: Phase 3 — Companion app pivot (5.0 workstream 2)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Produce a slimmed-down companion app build that ships native-dependent viewers (starting with LibreOffice embedding) without the full analysis UI, so the companion app stays lightweight.

## Problem

The full Swing analysis UI is too heavy to ship as a companion app whose sole job is to provide native-only viewer capabilities to the browser UI.

## Acceptance criteria

- [ ] A build variant exists that includes only native-dependent viewers, starting with LibreOffice embedding.
- [ ] The slim build excludes the full Swing analysis UI.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-app-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
