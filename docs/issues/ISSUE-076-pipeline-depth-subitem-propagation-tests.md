# ISSUE-076: Multi-stage pipeline depth and sub-item propagation correctness

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 8 — Pipeline depth and sub-item propagation
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Verify completion-counting correctness across a multi-stage task pipeline, and verify that sub-items discovered mid-pipeline correctly extend the case's completion criteria instead of allowing premature completion.

## Problem

`CaseCompletionMonitor` must only count `completedFinal` at the last stage of a multi-task pipeline, and must correctly grow `discovered` when sub-items are found partway through processing — both are easy to get wrong and would otherwise cause premature CASE_COMPLETED events.

## Acceptance criteria

- [x] `MultiStagePipelineTest` verifies a 3-task pipeline (HashTask->SignatureTask->IndexTask) routes items correctly; `isFinalStage` only fires `completedFinal` at the last stage; intermediate completions don't count; in-flight tracking is per-item-per-task (flight key = uuid|taskType); timeouts at intermediate stages emit TIMEOUT without incrementing `completedFinal`. 10 broker-free tests.
- [x] `SubitemDiscoveryTest` verifies `SUBITEM_DISCOVERED` events grow `discovered` after initial registration, preventing premature completion; sub-items discovered during processing hold the case open; multiple sub-items from the same parent each require independent completion; cases with only sub-items complete only when all children clear the final stage. 9 broker-free tests.
- [x] Full suite: 514 tests, 0 failures.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 8), grouping the multi-stage pipeline test and sub-item discovery test into one issue. Status set to `done`.
