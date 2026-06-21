# ISSUE-372: Open/close multiple cases per server instance

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 2 — Multi-case and session model
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Allow a single `iped-webapi` server instance to manage multiple open cases at once, with endpoints to list, open, inspect, and close them.

## Problem

The server previously assumed a fixed, startup-time set of sources; analysts need to manage cases dynamically without restarting the server.

## Acceptance criteria

- [x] `GET /v2/cases`, `POST /v2/cases`, `DELETE /v2/cases/{id}`, `GET /v2/cases/{id}` implemented in `CasesV2.java`.
- [x] Caller-chosen ID or UUID-from-path fallback supported; `POST` returns 201, duplicate ID returns 409.
- [x] Item count surfaced via `IIPEDSource.getTotalItens()`.
- [x] Backed by `SourceCatalogService.removeSource()` SPI method (soft-remove from string/int maps, closes handle, leaves positional multi-source slot intact for in-flight queries).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
