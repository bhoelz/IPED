# ISSUE-236: Case queue with priorities and concurrency limits

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 2 — Scheduling and queueing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`RunQueueService` wraps a `PriorityBlockingQueue<QueuedRun>` ordered by `RunPriority` (HIGH/NORMAL/LOW) and enqueue time, with a drainer thread and a `Semaphore` bounding concurrent execution.

## Problem

The runner needed to queue and prioritize cases rather than launching every submitted run immediately, to bound concurrency on shared hardware.

## Acceptance criteria

- [x] `RunQueueService` orders queued runs by priority then enqueue time.
- [x] A `Semaphore` with `runner.max-concurrent` permits bounds concurrent execution.
- [x] `POST /run` enqueues rather than launching directly.
- [x] `GET /queue` returns pending (not-yet-started) runs.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
