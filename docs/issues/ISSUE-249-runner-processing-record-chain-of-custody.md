# ISSUE-249: ProcessingRecord immutable chain-of-custody record

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Kafka security and chain-of-custody audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`ProcessingRecord` is an immutable record created from terminal `StatusEvent` types (COMPLETED, ERROR, TIMEOUT), forming the unit stored in the chain-of-custody audit log.

## Problem

The audit log needed a well-defined, immutable representation of "what happened to this item" derived only from terminal events.

## Acceptance criteria

- [x] `ProcessingRecord` is created from terminal `StatusEvent` types only.
- [x] Returns null for non-terminal events.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
