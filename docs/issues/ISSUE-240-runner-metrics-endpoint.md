# ISSUE-240: Metrics endpoint

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 3 — Operations
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`MetricsController` exposes `GET /metrics` returning flat JSON covering run/queue/profile counts, distributed/Kafka availability, and JVM heap/uptime stats.

## Problem

There was no machine-readable way to monitor runner health and load, which operators need for dashboards and alerting.

## Acceptance criteria

- [x] `GET /metrics` returns `runner.active_runs`, `runner.queued_runs`, `runner.slots_total`, `runner.slots_available`, `runner.profiles_available`.
- [x] Response includes `distributed.active_cases` and `distributed.kafka_available`.
- [x] Response includes `jvm.heap_used_bytes`, `jvm.heap_max_bytes`, `jvm.uptime_ms`, `ts`.
- [x] Kafka metrics are absent (not erroring) when `DistributedStatusService` is not in context.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
