# ISSUE-338: Validate config saves using the same engine-side validators

- Status: planned
- Roadmap: [iped-ui-ROADMAP.md](../roadmaps/iped-ui-ROADMAP.md)
- Roadmap section: Phase 1 — If kept (interim hardening)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Validate configuration edits before save using the same validators the engine uses, avoiding a second, divergent set of validation rules in the UI.

## Problem

If the UI implements its own validation logic separately from the engine, the two can drift apart, allowing invalid config to be saved or rejecting config the engine would actually accept.

## Acceptance criteria

- [ ] Config save path runs validation through the same engine-side validators used elsewhere.
- [ ] No divergent UI-only validation rules remain.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ui-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
