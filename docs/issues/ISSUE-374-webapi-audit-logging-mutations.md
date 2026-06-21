# ISSUE-374: Audit logging of all mutating operations

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 2 — Multi-case and session model
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Record every mutating REST operation to an audit log, both as a JSONL file and via SLF4J, including business-level case open/close events.

## Problem

Without audit logging, there was no record of who performed mutating operations (case open/close, writes) against the API, a compliance gap for a forensics tool.

## Acceptance criteria

- [x] `AuditLogger.java` writes JSONL to `audit.jsonl` (configurable via `iped.webapi.audit-log`) and echoes to SLF4J.
- [x] `AuditLoggingFilter.java` — Jersey filter at `AUTHENTICATION + 1000` records all POST/PUT/PATCH/DELETE requests with path and redacted principal.
- [x] `CasesV2` calls `AuditLogger.log("case.open/close", ...)` directly for business-level events.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
