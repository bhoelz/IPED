# ISSUE-251: AuditController endpoints for chain-of-custody records

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Kafka security and chain-of-custody audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`AuditController` exposes the chain-of-custody audit log over HTTP: JSON listing per case, RFC 4180 CSV download, and a per-item latest-terminal-record lookup, all 404 when no records exist.

## Problem

Audit data needed to be retrievable by API consumers and exportable for offline review/compliance purposes.

## Acceptance criteria

- [x] `GET /api/v1/audit/{caseId}` returns JSON, newest-first.
- [x] `GET /api/v1/audit/{caseId}/csv` returns an RFC 4180 CSV download.
- [x] `GET /api/v1/audit/{caseId}/{itemUuid}` returns the latest terminal record.
- [x] All three return 404 when no records exist.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
