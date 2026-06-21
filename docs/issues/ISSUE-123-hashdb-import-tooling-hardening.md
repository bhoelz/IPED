# ISSUE-123: Harden hash DB import tooling with resumable imports and validation

- Status: planned
- Roadmap: [iped-engine-hashdb-ROADMAP.md](../roadmaps/iped-engine-hashdb-ROADMAP.md)
- Roadmap section: Phase 2 — Performance and formats
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The hash DB import tooling needs hardening: support resumable imports for large source sets, and produce validation reports flagging malformed source data.

## Problem

A failed or interrupted import of a large hash set (e.g. NSRL, LedDB) currently likely has to restart from scratch, and malformed input rows may go unnoticed.

## Acceptance criteria

- [ ] Import tooling supports resuming after interruption instead of restarting fully.
- [ ] Import produces a validation report listing malformed records in the source set.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-hashdb-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
