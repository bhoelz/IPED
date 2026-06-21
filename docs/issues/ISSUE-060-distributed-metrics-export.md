# ISSUE-060: Consumer-lag, throughput, and per-stage error metrics export

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 2 — Operability
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Export consumer-lag, throughput, and per-stage error metrics in Prometheus format from the coordinator.

## Problem

Operators had no quantitative visibility into distributed processing throughput, error rates, or consumer lag, making it hard to detect stalls or regressions in production.

## Acceptance criteria

- [x] `DistributedMetrics` maintains thread-safe `LongAdder` counters keyed by (case, task-type, stage): `items_processed_total`, `items_failed_total`, `processing_duration_ms_total`.
- [x] Coordinator's status-event callback fans out to both `CaseCompletionMonitor` and `DistributedMetrics.recordEvent()`.
- [x] `ConsumerLagProvider` queries `AdminClient` for log-end vs committed offsets at scrape time; returns an empty map (not an error) when Kafka is unavailable.
- [x] `GET /metrics` serves Prometheus text-format with counters, per-agent in-flight/free gauges from `AgentRegistry`, and optional consumer-lag gauges for all active cases.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 2), status set to `done`.
