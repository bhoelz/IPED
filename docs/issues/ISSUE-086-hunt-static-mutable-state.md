# ISSUE-086: Hunt remaining static mutable state under concurrent-case load

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 2 — Multi-case hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Audit and fix remaining static mutable fields in the engine that are unsafe under concurrent multi-case processing.

## Problem

Several classes carried static or otherwise shared mutable state that could leak or corrupt data across concurrently running cases.

## Acceptance criteria

- [x] `Worker.workerNamePrefix` made `final`.
- [x] `Manager.commitIntervalMillis` converted to an instance field.
- [x] `EvidenceStatus` path constants made `final`.
- [x] `QueuesProcessingOrder.mediaTypes` converted to `ConcurrentHashMap`; `mediaRegistry` made `volatile`.
- [x] `IPEDMultiSource.baseDocCache` converted from `static ArrayList` to an instance field.
- [x] `Item.extraAttributeSet` made `final`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `done`.
