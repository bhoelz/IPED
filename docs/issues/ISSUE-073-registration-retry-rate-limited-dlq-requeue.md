# ISSUE-073: Agent registration retry and rate-limited DLQ requeue

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 5 — Production hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add exponential-backoff retry to agent registration against the coordinator, and let DLQ requeue apply a configurable delay before redelivery instead of always requeuing immediately.

## Problem

Agent registration previously was fire-and-forget best-effort, with no resilience against transient coordinator unavailability at agent startup. Separately, DLQ requeue always redelivered immediately, with no way to apply a cool-down before retrying a failed item.

## Acceptance criteria

- [x] `CoordinatorClient.registerWithRetry(reg, maxAttempts, baseDelayMs)` retries with exponential backoff (delay = base x 2^attempt, capped at bit-shift 10 to avoid overflow); throws `RuntimeException` after exhausting all attempts; existing `register()` remains a fire-and-forget fallback. 3 broker-free tests.
- [x] `DlqManager.requeue(caseId, positions, delayMs)` sets `notBeforeMs = now + delayMs` when `delayMs > 0`, otherwise immediate (`notBeforeMs = 0`); zero-arg overload delegates for backward compatibility.
- [x] `POST /api/v1/dlq/{caseId}/requeue` parses the optional `delayMs` body field and echoes it in the response.
- [x] `DlqManager.count(caseId)` returns the total unconsumed entry count across all DLQ partitions without committing.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 5), grouping agent registration retry with rate-limited DLQ requeue. Status set to `done`.
