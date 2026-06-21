# ISSUE-327: Untangle UFED parser-side coupling via a shared artifact model

- Status: planned
- Roadmap: [iped-ufed-ROADMAP.md](../roadmaps/iped-ufed-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary and registration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

UFED chat/contact rendering currently living in `iped-parsers` should consume a shared artifact model rather than reaching into UFED reader internals, removing cross-module coupling between `iped-parsers` and `iped-ufed`.

## Problem

`iped-parsers` likely depends on internal UFED reader types directly to render chats/contacts, which breaks the module boundary established for `iped-ufed`.

## Acceptance criteria

- [ ] A shared artifact model is defined for UFED chat/contact data.
- [ ] `iped-parsers` consumes the shared model instead of UFED reader internals.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ufed-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
