# ISSUE-363: Expose native-only viewers through the companion app bridge

- Status: planned
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 3 — Companion app bridge (5.0 workstream 2)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Expose native-only viewers, starting with LibreOffice embedding, through the companion desktop app handoff protocol, with a documented retirement criterion for each bridged viewer.

## Problem

Some viewers (LibreOffice-embedded documents, OS-shell-based file handling) cannot be ported to the browser and instead need a handoff protocol to a companion desktop app, which doesn't exist yet.

## Acceptance criteria

- [ ] LibreOffice embedding viewer exposed through the companion desktop app handoff protocol.
- [ ] Each bridged native-only viewer gets a documented retirement criterion.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
