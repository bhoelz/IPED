# ISSUE-252: Multi-node agent inventory (health/heartbeats) delivered

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Multi-node agent inventory
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`AgentHeartbeatEvent`, `AgentRecord`, `AgentInventoryService`, and `AgentController` together deliver the multi-node agent inventory view that was originally deferred in Phase 3 (see ISSUE-242). Health is computed at call time against `Instant.now()`: active (<30s), stale (30-120s), offline (>120s).

## Problem

The runner needed to track multiple distributed agents' liveness and capacity so operators and the dashboard could see fleet health at a glance.

## Acceptance criteria

- [x] `AgentInventoryService` keeps the latest heartbeat per `agentId` via `ConcurrentHashMap.merge()`.
- [x] `listAgents()` recomputes health at call time.
- [x] `GET /v2/agents` returns `{enabled, total, active, stale, offline, agents[]}`, empty (not 503) when Kafka is absent.
- [x] Disabled gracefully when Kafka is not configured (same bootstrap-servers gate as `DistributedStatusService`).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker. This entry duplicates the capability tracked as deferred under Phase 3 in ISSUE-242; both now point at the same completed implementation.
