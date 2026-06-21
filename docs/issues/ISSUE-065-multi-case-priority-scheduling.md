# ISSUE-065: Multi-case scheduling across the agent pool with priorities

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 3 — Scale and scheduling
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Distribute free agent slots across active cases by priority, letting urgent cases preempt queue order (not running segments), with fair round-robin sharing within a priority class.

## Problem

Without prioritized scheduling, urgent cases would have to wait in line behind lower-priority cases for free agent capacity, with no way for operators to express case urgency.

## Acceptance criteria

- [x] `iped.distributed.scheduler.CaseScheduler` distributes free agent slots by `CasePriority` (URGENT>HIGH>NORMAL>LOW): strict priority across classes, round-robin within a class, capped at pending work, free-slots-only (never preempts in-flight items).
- [x] Deterministic given (priorities, pending work, free slots).
- [x] Priority persisted in case definition (`CaseStatus.priority`, default NORMAL, survives coordinator restart).
- [x] REST: `priority` field on `POST /cases/start` and `POST /api/v1/cases/{caseId}/priority` to re-prioritise.
- [x] Coordinator feeds the scheduler per task type from `CaseLifecycleManager` priorities + `ConsumerLagProvider` pending work + `AgentRegistry` free slots.
- [x] Documented in `specs/91-multi-case-scheduling.md`; 14 scheduler tests + 3 priority-persistence tests.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 3), status set to `done`. Agent-side enforcement (steering a typed agent across case topics per the plan) is tracked separately under agent steering (see ISSUE-070).
