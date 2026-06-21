# ISSUE-027: Add API-level event/listener contracts for the distributed pipeline

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 2 — Versioned API for IPED 5.0
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add API-level event/listener contracts for the distributed pipeline (job lifecycle, item processed, case completed) so that `iped-distributed` and `iped-runner` don't bind directly to engine internals.

## Problem

Without dedicated API-level event contracts, the distributed processing modules would need to depend on engine-internal types to observe pipeline lifecycle events, undermining the module boundary.

## Acceptance criteria

- [x] `iped.pipeline.IJobLifecycleListener` added.
- [x] `iped.pipeline.IItemProcessingListener` added.
- [x] `iped.pipeline.JobLifecycleEvent` added.
- [x] `iped.pipeline.ItemProcessingEvent` added.
- [x] `iped.pipeline.JobPhase` added.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
