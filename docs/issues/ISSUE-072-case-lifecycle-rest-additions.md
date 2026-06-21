# ISSUE-072: Case lifecycle REST additions — deletion, progress, work-unit tagging, error tracking

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 5 — Production hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Round out the coordinator's case-lifecycle REST surface: case deletion, per-case progress/health/DLQ-count endpoints, work-unit ID tagging on messages, and per-case error/failed-count tracking.

## Problem

Operators and dashboards needed a richer set of coordinator endpoints to manage case lifecycle (deletion) and observe progress/health/failure counts without pulling full DLQ entries or building custom tooling.

## Acceptance criteria

- [x] Work-unit tagging: `KafkaItemMessage` gains `workUnitId` (string) and `workUnitIndex` (int) fields, assigned by `AdaptiveWorkUnitPlanner` at ingestion time; both `@JsonIgnoreProperties`-safe. 4 broker-free tests.
- [x] Case deletion: `DELETE /api/v1/cases/{caseId}` deprovisions Kafka topics via `TopicProvisioner.deprovisionCase()` and removes the case from all lifecycle maps; `CaseLifecycleManager.deleteCase()` returns `false` for unknown cases (mapped to HTTP 404); `cases.json` updated atomically. 3 broker-free tests.
- [x] `GET /api/v1/cases/{caseId}/progress` — `{discovered, completedFinal, inFlight, failed, completionPct}`; 404 for unknown cases.
- [x] `GET /api/v1/health` — `{status:"ok", activeCases, liveAgents, uptimeMs}`.
- [x] `GET /api/v1/dlq/{caseId}/count` — lightweight entry count via begin-to-end offset difference.
- [x] Error tracking: `CaseProgress.failedCount` (`AtomicLong`) incremented on every ERROR status event, exposed via `failedCount(caseId)` and surfaced in the progress endpoint's `failed` field. 4 broker-free tests.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 5), grouping case deletion, the new REST progress/health/DLQ-count endpoints, work-unit tagging, and error tracking into one issue. Status set to `done`.
