# ISSUE-266: Dashboard stream SSE replaces job-list polling

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 3 — Hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`/dashboard/stream` SSE replaces the 3-second job-list poll; a 10-second fallback poll remains for queue/agents/metrics, and the `jobs-update` event fires an immediate `fetchJobs()`.

## Problem

Polling the job list every 3 seconds was wasteful once an SSE channel was available; the UI needed to switch to event-driven updates for the job list specifically.

## Acceptance criteria

- [x] `/dashboard/stream` SSE replaces the job-list poll.
- [x] 10s fallback poll remains for queue/agents/metrics.
- [x] `jobs-update` event triggers an immediate `fetchJobs()`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
