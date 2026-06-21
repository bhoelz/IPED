# ISSUE-070: Multi-case agent steering with dynamic topic subscription and cap

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 5 — Production hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Let the coordinator steer a multi-case agent's Kafka subscriptions dynamically based on which RUNNING cases need its task type, with an optional cap that prioritises the highest-priority cases when an agent can't subscribe to everything.

## Problem

A single agent process needs to serve multiple cases of its task type, but the set of relevant case topics changes as cases start, pause, and complete — and an agent might not be able to subscribe to unlimited topics, requiring a priority-aware cap.

## Acceptance criteria

- [x] Coordinator heartbeat response carries `subscribedTopics`, computed from all RUNNING cases that include the agent's task type; agents apply the update via a volatile `pendingTopicUpdate` handoff on the next poll iteration.
- [x] New `TaskAgent(taskType, stageNumber, ...)` 5-arg constructor creates a multi-case agent with consumer group = `taskType`; existing 6-arg single-case constructor preserved for backward compatibility.
- [x] Dynamic topic derivation in `processRecord()`, `handleFailure()`, `sendToDeadLetterQueue()`, `buildSubitemRegistry()` uses the message's own `caseId`.
- [x] New `HeartbeatResponse` DTO; `CoordinatorClient.heartbeat()` returns it; new `AgentTopicAssigner` (excludes COMPLETED/FAILED cases; sorts topics deterministically); `AgentRegistry.getAgent(agentId)` accessor.
- [x] `DistributedConfig.maxSubscribedCases` (default 0 = unlimited) caps topics returned per multi-case agent, selecting highest-priority running cases (by `CasePriority.weight()` descending, then case ID ascending) before mapping to topics; result sorted lexicographically.
- [x] 21 broker-free tests in `AgentSteeringTest`; 6 broker-free cap tests in `Phase5HardeningTest`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 5), grouping agent-side multi-case dynamic subscription with the scheduler-driven subscription cap. Status set to `done`.
