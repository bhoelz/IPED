# ISSUE-241: Webhook notifications on run completion/failure

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 3 — Operations
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`WebhookNotifier` fires a fire-and-forget HTTP POST on every terminal run event (COMPLETED, FAILED, ABORTED, TIMED_OUT), configured via `runner.webhook.url` and an optional signed secret.

## Problem

Operators needed to be notified externally (e.g. by an integration system) when a run finished, without polling the runner API.

## Acceptance criteria

- [x] `WebhookNotifier` posts on every terminal run event.
- [x] Configurable via `runner.webhook.url` and optional `runner.webhook.secret` (sent as `Authorization: Bearer`).
- [x] Delivery failures are logged at WARN and never affect run state.
- [x] Wired into `ExecutionService.recordTerminal()` after history persistence.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
