# ISSUE-250: ProcessingAuditLog with CSV export and optional persistence

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Kafka security and chain-of-custody audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`ProcessingAuditLog` stores `ProcessingRecord`s per case in a `ConcurrentHashMap<caseId, CopyOnWriteArrayList<ProcessingRecord>>`, with lookup, CSV export, and optional append-mode file persistence, wired into `DistributedStatusService.handleMessage()`.

## Problem

Audit records needed an in-memory store with query and export capabilities, plus an optional durable persistence path.

## Acceptance criteria

- [x] `record()`, `forCase()`, `forItem()`, `latestForItem()`, `exportCsv()` implemented.
- [x] Optional file persistence via append-mode CSV (`ProcessingAuditLog(Path)` constructor).
- [x] Wired into `DistributedStatusService.handleMessage()`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
