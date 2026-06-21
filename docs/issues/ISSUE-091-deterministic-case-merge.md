# ISSUE-091: Deterministic case merge of distributed segment outputs

- Status: blocked
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed-processing integration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Combine distributed segment outputs into one case index with verifiable counts, tying into `iped-runner` completion detection.

## Problem

Distributed processing splits a case into segments processed independently; merging their outputs deterministically into a single verifiable case index is not yet implemented. This requires `iped-runner` and distributed segment-output infrastructure outside this branch's scope.

## Acceptance criteria

- [ ] Define the segment-output merge contract with `iped-runner`.
- [ ] Implement deterministic merge of segment outputs into one case index.
- [ ] Verify merged item counts against the sum of segment counts.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`. Status set to `blocked` — deferred pending `iped-runner` and distributed segment-output infrastructure outside this branch's scope.
