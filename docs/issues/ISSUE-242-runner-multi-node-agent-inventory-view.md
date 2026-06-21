# ISSUE-242: Multi-node awareness — agent inventory view with health/heartbeats

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 3 — Operations
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

This item was originally deferred out of Phase 3 pending a Kafka heartbeat topic convention, and was delivered later under Phase 5 (see ISSUE-252) via `AgentInventoryService`/`AgentController`.

## Problem

Operators had no visibility into which distributed agents were online, stale, or offline.

## Acceptance criteria

- [x] Agent inventory view with computed health (active/stale/offline) is available via `GET /v2/agents`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done`. Originally tracked as deferred (`[ ]`) in Phase 3 pending the Kafka heartbeat topic convention; the underlying work was completed under Phase 5 (see ISSUE-252) and both roadmap entries refer to the same delivered capability.
