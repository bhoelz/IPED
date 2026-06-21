# ISSUE-090: Idempotent, replay-safe item processing for Kafka retry/DLQ semantics

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed-processing integration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Ensure reprocessing a segment (as happens under Kafka retry/DLQ semantics) cannot duplicate index entries or IDs.

## Problem

Distributed retry/DLQ flows can redeliver the same item more than once. Without idempotency tracking, this risked duplicate index entries or IDs.

## Acceptance criteria

- [x] Add `CaseContext.processedTrackIds` (`ConcurrentHashMap.newKeySet()`).
- [x] `Worker.processNewItem` checks `processedTrackIds.add(trackId)` before queuing; duplicates logged at DEBUG and skipped.
- [x] Queue-end sentinels bypass the dedup check.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `done`.
