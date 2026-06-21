# ISSUE-037: Keep multi-case/processing UI working as the engine evolves

- Status: planned
- Roadmap: [iped-app-ROADMAP.md](../roadmaps/iped-app-ROADMAP.md)
- Roadmap section: Phase 2 — Maintenance mode for Swing analysis UI
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Ensure the multi-case/processing UI continues to work as the engine evolves, which requires decoupling `UIPropertyListener` from engine internals. This work is deferred and tracked primarily in the engine roadmap.

## Problem

`UIPropertyListener` currently has a tight coupling to engine internals, making the processing UI fragile to engine-side refactors.

## Acceptance criteria

- [ ] `UIPropertyListener` decoupled from engine internals.
- [ ] Multi-case/processing UI verified to keep working across engine refactors.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-app-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker (decoupling work deferred and tracked in the engine roadmap).
