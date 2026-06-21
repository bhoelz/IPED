# ISSUE-075: Multi-agent chaos simulation and coordinator lifecycle integration tests

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 7 — Resilience validation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Validate end-to-end resilience with a multi-agent crash-injection chaos simulation and a coordinator lifecycle integration suite that exercises pause/resume, multi-case isolation, deletion, priority capping, stall detection, and full coordinator-restart recovery together.

## Problem

Unit tests validate individual components in isolation, but the distributed pipeline's resilience guarantees (no item loss, no duplicates, correct completion detection under crashes, coherent multi-step coordinator behavior) can only be confirmed by driving multiple components through coherent, realistic multi-step scenarios.

## Acceptance criteria

- [x] `MultiAgentChaosTest`: N agents each owning a partition, K agents killed mid-run; verifies no item loss, no duplicates (upsert semantics), and correct case completion despite crash-and-restart cycles. Covers no-crash baseline, single crash, all-agents crash, repeated crash on same agent, sub-item UUID stability, upsert idempotency, watermark gap-stop+fill. 10 broker-free tests.
- [x] `CoordinatorLifecycleIntegrationTest` drives the five coordinator components through 10 coherent multi-step scenarios: happy-path completion, pause/resume topic reassignment, multi-case isolation, case deletion topic/lifecycle cleanup, priority-cap top-N selection, CASE_COMPLETED event-log emission, stall-detection event emission, agent-expiry eviction, independent multi-case completion, and full coordinator-restart recovery. 10 broker-free tests.
- [x] Full suite: 495 tests, 0 failures.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 7), grouping the multi-agent chaos test and coordinator lifecycle integration test into one issue. Status set to `done`.
