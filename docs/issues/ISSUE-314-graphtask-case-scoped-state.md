# ISSUE-314: Move GraphTask static state into a per-case GraphAccumulator

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 3 — Distributed and multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`GraphTask`'s static `formattedPhonesCache` and `datasourceOwnerMap` (flagged as
`GLOBAL` in the classification matrix, ISSUE-312) were moved into a per-case
`GraphAccumulator`, with the intentionally cross-case phone-formatting cache left
static as a bounded LRU.

## Problem

`GraphTask` shared `datasourceOwnerMap` statically across cases, while
`commit()` is invoked reflectively by `Manager` and needed a way to resolve the
currently active case's accumulator without becoming case-unsafe itself.

## Acceptance criteria

- [x] `graphFileWriter` and `datasourceOwnerMap` moved into a per-case
      `GraphAccumulator` stored in `caseData`.
- [x] `formattedPhonesCache` intentionally left static (bounded LRU perf cache,
      cross-case reuse is intentional).
- [x] `commit()` kept static via a `volatile static activeAccumulator` forwarding
      reference, set in `accum()` and cleared in `finish()`.
- [x] `synchronized(this.getClass())` in `getGenericOwnerNode` replaced with
      `synchronized(accum())` for per-case isolation.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
