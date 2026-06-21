# ISSUE-277: All backend surface consumed by the UI

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 4 — Convergence (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

The frontend now consumes every backend endpoint: `/v2/jobs`, `/v2/agents`, `/queue`, `/metrics` (including JVM and distributed fields), `/profiles`, `POST /run`, `POST /runs/batch`, `DELETE /v2/jobs/{id}`, `/run/{id}/stream`, `/dashboard/stream`, `/api/v1/audit/{caseId}`, `/api/v1/audit/{caseId}/csv`, and `/api/v1/audit/{caseId}/{itemUuid}`.

## Problem

There was a risk of backend endpoints existing without any corresponding UI surface, leaving capability unreachable by operators.

## Acceptance criteria

- [x] Every listed backend endpoint has a corresponding UI feature consuming it.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
