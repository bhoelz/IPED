# ISSUE-245: Agent heartbeat inventory service

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Kafka security and chain-of-custody audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`AgentHeartbeatEvent` mirrors `iped.agents` Kafka topic messages, `AgentRecord` computes health (active/stale/offline) from last-seen timestamps, and `AgentInventoryService`/`AgentController` expose this via `GET /v2/agents`.

## Problem

Distributed agents needed a centrally observable heartbeat inventory for operational visibility and the chain-of-custody audit work.

## Acceptance criteria

- [x] `AgentInventoryService` runs a dedicated consumer group per runner instance and updates a `ConcurrentHashMap`.
- [x] `GET /v2/agents` returns `{enabled, total, active, stale, offline, agents[]}`.
- [x] Returns an empty list (not 503) when Kafka is absent.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
