# ISSUE-063: Coordinator failover story

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 2 — Operability
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Document and test what happens when the coordinator goes down and recovers, given that it is not in the data path. Persist case definitions and replay the status topic to rebuild progress on restart.

## Problem

The coordinator holds case-definition state (the ordered task list) that cannot be reconstructed purely from Kafka status events, so a coordinator outage risked losing that state permanently or double-publishing completion events on recovery. Multi-coordinator HA (leader election) is explicitly out of scope for this phase.

## Acceptance criteria

- [x] Key insight documented: the coordinator is NOT in the data path, so agents keep processing across a coordinator outage; only observability/auto-detection pauses.
- [x] `CaseLifecycleManager` optionally persists case definitions to `cases.json` via `coordinatorStateDir` config, reloaded on startup.
- [x] `CaseCompletionMonitor.recover()` replays the `iped.status` topic (bounded, throwaway group, no commits — `StatusTopicReplayer`) to rebuild completion progress with side effects suppressed (no re-published CASE_COMPLETED/TIMEOUT).
- [x] `AgentRegistry` self-heals via heartbeats.
- [x] Documented in `specs/89-coordinator-failover.md`; 13 broker-free tests in `CoordinatorFailoverTest`.
- [x] Multi-coordinator HA (leader election) explicitly documented as out of scope / Phase 4 follow-up — run one supervised coordinator.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 2), status set to `done`.
