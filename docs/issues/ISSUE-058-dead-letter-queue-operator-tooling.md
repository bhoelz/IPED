# ISSUE-058: Dead-letter queue with operator tooling

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 1 — Correctness guarantees
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Provide a dead-letter queue with operator-facing list/requeue/discard tooling, exposed via coordinator REST endpoints.

## Problem

Failed items needed a durable holding area an operator could inspect and act on, rather than being silently dropped or stuck retrying indefinitely.

## Acceptance criteria

- [x] `DlqManager` (list/requeue/discard) implemented and wired into `CoordinatorServer`.
- [x] `GET /api/v1/dlq/{caseId}?limit=N` — stateless peek (random group, no commit).
- [x] `POST /api/v1/dlq/{caseId}/requeue` (body: `{"positions":[...]}`) — resets `attempt=0` and `notBeforeMs=0`, commits the DLQ offset in the `iped.dlq.ops.{caseId}` consumer group.
- [x] `POST /api/v1/dlq/{caseId}/discard` (body: `{"positions":[...]}`) — commits the DLQ offset without forwarding.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 1), status set to `done`.
