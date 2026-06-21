# ISSUE-264: Agent inventory panel for distributed runs

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 2 — Launch and queue management
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`AgentsPanel` calls `GET /v2/agents` and shows active/stale/offline health badges, hostname, version, task count, and last-seen, with a graceful "Kafka not configured" message when disabled.

## Problem

The UI needed a way to show distributed agent health and load alongside run/queue views.

## Acceptance criteria

- [x] `AgentsPanel` displays health badges, hostname, version, task count, last-seen.
- [x] Shows a graceful message when Kafka/agents reporting is disabled.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
