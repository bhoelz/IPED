# ISSUE-278: Browser notifications on job completion

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 5 — Operator UX polish
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A bell button in the metrics bar drives a `Notification.requestPermission()` flow; `sendNotification()` fires on job done/failed/aborted from the SSE finish handler, with a state-aware icon (bell granted, bellOff default/denied) and a disabled state when permissions are blocked.

## Problem

Operators monitoring long-running jobs from another tab/window had no way to be notified when a run finished without keeping the dashboard tab focused.

## Acceptance criteria

- [x] Bell button drives `Notification.requestPermission()`.
- [x] Notification fires on job done/failed/aborted.
- [x] Icon and button state reflect granted/default/denied permission.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
