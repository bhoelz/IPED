# ISSUE-375: Async job API for export and report generation

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 3 — Jobs and streaming
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add an asynchronous job API for long-running operations like export and report generation, with status polling and an in-memory job registry.

## Problem

Export and report generation can take a long time; the API needed an async pattern instead of blocking requests, plus a way for clients to poll status.

## Acceptance criteria

- [x] `JobRegistry.java` — in-memory store keyed by UUID; `JobEntry` tracks status (pending→running→completed/failed/cancelled), 0–100 progress, message, and SSE subscriber consumers; terminal entries GC'd after 5 min.
- [x] `JobsV2.java` — `POST /v2/jobs`, `GET /v2/jobs`, `GET /v2/jobs/{id}`, `DELETE /v2/jobs/{id}` (cancel).
- [x] Supported job types: `export` (ZIP selected items via `IItem.getBufferedInputStream()`) and `report` (HTML/PDF placeholder delegating to the engine's ReportTask).
- [x] All mutations audited via `AuditLogger`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
