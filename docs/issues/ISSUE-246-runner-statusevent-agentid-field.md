# ISSUE-246: agentId field added to StatusEvent for provenance

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Kafka security and chain-of-custody audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`StatusEvent` is stamped with the publishing agent's `agentId`, so chain-of-custody audit records carry full provenance back to the originating agent.

## Problem

Audit records needed to trace which agent produced a given processing event, which was not previously captured.

## Acceptance criteria

- [x] `StatusEvent` includes an `agentId` field stamped by the publishing agent.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
