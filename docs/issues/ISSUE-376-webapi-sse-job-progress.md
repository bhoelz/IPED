# ISSUE-376: SSE channel for real-time job progress

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 3 — Jobs and streaming
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Provide a Server-Sent Events endpoint so clients can watch job progress in real time instead of polling the job-status endpoint.

## Problem

Polling for job status is wasteful and adds latency; clients needed a push-based way to observe job progress.

## Acceptance criteria

- [x] `JobsSseV2.java` — `GET /v2/jobs/{id}/events` (text/event-stream).
- [x] Subscribes a consumer to `JobEntry.subscribe()`; immediately delivers current state so the client never polls first.
- [x] Stream closes automatically on terminal event.
- [x] Named events emitted: `started`, `progress`, `completed`, `failed`, `cancelled`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
