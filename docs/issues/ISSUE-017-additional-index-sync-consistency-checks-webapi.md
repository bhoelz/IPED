# ISSUE-017: Sync/consistency checks reportable via iped-webapi

- Status: planned
- Roadmap: [iped-additional-index-ROADMAP.md](../roadmaps/iped-additional-index-ROADMAP.md)
- Roadmap section: Phase 3 — Operations
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Operators need visibility into whether each additional store is in sync with the case index — comparing per-store document counts against case item counts — surfaced through `iped-webapi`.

## Problem

There is no reporting mechanism today to detect drift between an additional store's contents and the authoritative case index.

## Acceptance criteria

- [ ] A consistency check compares per-store document counts to case item counts.
- [ ] Results are exposed through an `iped-webapi` endpoint.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-additional-index-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
