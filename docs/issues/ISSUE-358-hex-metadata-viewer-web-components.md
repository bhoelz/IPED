# ISSUE-358: Hex viewer and metadata viewer as production web components

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 2 — Web viewer buildout (5.0 workstream 1)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Promote the hex viewer and metadata viewer from prototyped SSR workspace fragments to real, production web component implementations.

## Problem

The hex and metadata viewers existed only as prototypes in the SSR workspace fragments and needed to become real implementations backed by actual endpoints rather than stub data.

## Acceptance criteria

- [x] `<iped-hex-viewer>` implemented as a production Angular island with byte-range pagination via `ContentV2` (using the Range: header support added in iped-webapi Phase 3).
- [x] Metadata panel served as the HTMX info-panel fragment via `WorkspaceFragmentController.info()`, backed by a real endpoint with no stub data.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
