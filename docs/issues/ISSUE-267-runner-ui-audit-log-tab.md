# ISSUE-267: Chain-of-custody audit log tab

- Status: done
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 3 — Hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

A case ID search drives `GET /api/v1/audit/{caseId}`, rendering coloured outcome chips (COMPLETED/ERROR/TIMEOUT) and a CSV export link to `GET /api/v1/audit/{caseId}/csv`.

## Problem

The chain-of-custody audit data exposed by the backend needed a usable UI surface for operators to search and export.

## Acceptance criteria

- [x] Case ID search calls `GET /api/v1/audit/{caseId}`.
- [x] Outcome chips are colour-coded by COMPLETED/ERROR/TIMEOUT.
- [x] CSV export link calls `GET /api/v1/audit/{caseId}/csv`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
