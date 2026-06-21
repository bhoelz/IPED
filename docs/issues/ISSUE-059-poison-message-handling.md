# ISSUE-059: Poison-message handling

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 1 — Correctness guarantees
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Ensure a malformed evidence segment cannot stall a Kafka partition by detecting and routing poison messages directly to the dead-letter queue.

## Problem

A single malformed/unparseable message in a partition could otherwise block the entire partition's progress if not specifically handled.

## Acceptance criteria

- [x] `KafkaItemDeserializer` catches all parse exceptions and returns a sentinel instead of throwing.
- [x] `TaskAgent.processRecord` detects the sentinel via `KafkaItemDeserializer.isPoison()` and routes directly to the stage DLQ (attempt=0, no task execution), then commits the offset.
- [x] Sentinel carries `__poison.error` (exception message) and `__poison.rawSnippet` (hex prefix of raw bytes) in `extraAttributes` for operator inspection via `DlqManager.list`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 1), status set to `done`.
