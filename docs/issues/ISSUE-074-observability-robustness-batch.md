# ISSUE-074: Phase 6 observability batch — work-unit wiring, case ops, event log, DLQ stats

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 6 — Observability and robustness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Round out coordinator observability and case operability: wire adaptive work-unit planning into the producer, add case pause/resume/tags/batch-status, track per-case stall detection, add a coordinator event log, agent pool summary/detail endpoints, per-topic DLQ stats, and a DLQ auto-retrier background service.

## Problem

Operators needed deeper visibility into and control over running cases and the DLQ beyond the Phase 5 hardening surface: pausing/resuming cases, tagging them with metadata, detecting silently stalled cases, viewing a coordinator event timeline, inspecting agent pool state, and automatically retrying eligible DLQ entries instead of requiring manual requeue every time.

## Acceptance criteria

- [x] Work-unit producer wiring: `KafkaItemProducer` buffers published items and calls `AdaptiveWorkUnitPlanner.plan(buffer)` on each add; completed units flushed immediately, a final `flush()` sends any remaining partial unit; items tagged with `workUnitId`/`workUnitIndex`. 5 broker-free tests.
- [x] Case pause/resume: `CaseLifecycleManager.pauseCase`/`resumeCase` with idempotent happy-path transitions and `false` for invalid transitions; `POST /api/v1/cases/{caseId}/pause` and `/resume`; `AgentTopicAssigner` already filters to RUNNING-only so paused cases stop being assigned automatically. 5+3 tests.
- [x] Case tags/metadata: `CaseLifecycleManager.updateTags` merges into `CaseStatus.metadata`, persisted atomically; `PATCH /api/v1/cases/{caseId}/tags`. 4 broker-free tests.
- [x] Case stall detection: `CaseProgress.isStalled(caseId, stallWindowMs)` using `lastEventAtMs`. 6 broker-free tests.
- [x] Coordinator event log: `CoordinatorEventLog` ring buffer (default capacity 500) of `CoordinatorEvent`; `EventType` covers 13 lifecycle transitions; `GET /api/v1/events?limit=N`. 8 broker-free tests.
- [x] Agent pool endpoints: `GET /api/v1/agents/summary` (grouped by taskType/state) and `GET /api/v1/agents/{agentId}` (404 for unknown).
- [x] DLQ per-topic stats: `DlqManager.topicStats(caseId)` via `AdminClient.listOffsets`; `GET /api/v1/dlq/{caseId}/stats`.
- [x] DLQ auto-retrier: `DlqAutoRetrier` background service sweeps RUNNING cases and requeues entries under `dlqAutoRetryMaxAttempts`, controlled by three new `DistributedConfig` keys. 4 broker-free config tests.
- [x] Batch case status: `GET /api/v1/cases/batch-status?ids=c1,c2,...` returns state/discovered/completedFinal/failed/priority per case in one round-trip.
- [x] 35 broker-free tests in `Phase6ObservabilityTest`; full suite 475 tests, 0 failures.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 6), grouping all nine Phase 6 checkboxes into one issue covering work-unit wiring, case lifecycle operability, observability endpoints, and DLQ automation. Status set to `done`.
