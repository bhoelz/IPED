# ISSUE-062: Graceful agent drain for rolling restarts

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 2 — Operability
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Allow an agent to finish its current segment, commit, and exit cleanly on SIGTERM, enabling safe rolling restarts.

## Problem

Hard-stopping an agent mid-segment during a rolling restart risked losing in-flight work or missing final offset commits, and an existing shutdown-ordering bug could drop the final commit on any exit path.

## Acceptance criteria

- [x] `TaskAgent.drain()` sets a draining flag; the poll loop pauses all Kafka partitions (no new records), waits for `inFlight == 0`, calls `producer.flush()` so every async send callback (`markDone`) fires, then exits.
- [x] Fixed shutdown ordering bug: `producer.flush()` is now called before `drainCommittable()`+`commitSync()` in ALL exit paths (both drain and hard `stop()`).
- [x] `stop()` retains hard-exit semantics; `drain()` is for SIGTERM.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 2), status set to `done`.
