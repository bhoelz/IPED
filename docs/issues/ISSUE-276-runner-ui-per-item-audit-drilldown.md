# ISSUE-276: Per-item audit drill-down

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 4 — Convergence (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Clicking any audit table row fetches `GET /api/v1/audit/{caseId}/{itemUuid}` and expands an accordion row with the full `ProcessingRecord` fields, including the untruncated error message.

## Problem

The audit table only showed summary rows; operators needed to drill into the full record for a specific item without leaving the page.

## Acceptance criteria

- [x] Clicking a row fetches `GET /api/v1/audit/{caseId}/{itemUuid}`.
- [x] Accordion row expands with the full `ProcessingRecord` fields, including untruncated error message.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
