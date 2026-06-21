# ISSUE-057: At-least-once delivery with deterministic re-delivery idempotency

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 1 — Correctness guarantees
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Guarantee at-least-once Kafka delivery via manual offset commit, make sub-item re-delivery idempotent via deterministic UUID derivation, and validate that the combination yields exactly-once observable effect under simulated crashes.

## Problem

`TaskAgent` needed to switch from Kafka auto-commit to manual offset tracking so interrupted items are safely redelivered without being lost, and redelivered sub-items needed stable, deterministic identities so retries don't create duplicate index entries. The end-to-end guarantee then needed validation against simulated agent crashes at every commit point.

## Acceptance criteria

- [x] At-least-once delivery: `TaskAgent` switched from auto-commit to manual offset commit via `PartitionOffsetTracker`; offsets advance only after the forwarded record is durably acknowledged by the broker (or parked in DLQ); interrupted items are intentionally left uncommitted for re-delivery.
- [x] Re-delivery idempotency: sub-item UUIDs derived deterministically from `(parentItemUuid, ordinal)` via `ItemConverter.deterministicSubitemUuid()` (UUID v3, MD5 name-based, stable across restarts); `buildSubitemRegistry` assigns ordinals in emission order; parent items are naturally idempotent via Lucene `updateDocument`.
- [x] Exactly-once-effect validation: `ExactlyOnceEffectTest` simulates agent crashes at every commit point (0, 1, 3, 5, 10) against a monolithic baseline; verifies no items lost, no items duplicated, sub-item UUIDs stable across redeliveries, `PartitionOffsetTracker` watermark stops at the first uncommitted gap, and poison items DLQ'd without corrupting the output index.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 1), grouping the at-least-once delivery, re-delivery idempotency, and exactly-once-effect validation checkboxes into one issue. Status set to `done`.
