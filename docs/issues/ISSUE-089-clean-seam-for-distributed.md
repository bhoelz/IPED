# ISSUE-089: Clean seam for iped-distributed (job/segment lifecycle hooks, no Kafka types in engine)

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed-processing integration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Expose job/segment lifecycle hooks from the engine so `iped-distributed` can drive processing without the engine ever importing Kafka types.

## Problem

`iped-distributed` needs to observe job/segment lifecycle events, but the engine must remain free of any Kafka-specific dependency to preserve module boundaries.

## Acceptance criteria

- [x] Add `iped.engine.pipeline.EngineHooks`.
- [x] Expose hooks via `CaseContext.getHooks()`.
- [x] `iped-distributed` registers listeners via `IJobLifecycleListener`/`IItemProcessingListener` from `iped-api`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `done`.
