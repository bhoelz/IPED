# ISSUE-370: Startup hardening — validate --sources, support zero-case startup

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 1 — v2 contract completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Harden server startup so `--sources` is optional, invalid paths produce a clear error instead of an NPE, and the server can start with zero cases and open cases later via the API.

## Problem

`iped-webapi` previously required a real processed case at startup; an empty source list caused an NPE in `ConfigurationManager`, a known rough edge that blocked flexible deployment.

## Acceptance criteria

- [x] `--sources` made optional in `Main.java`.
- [x] `EngineSourceCatalogService.init()` treats null/blank as zero sources and starts with an empty `IPEDMultiSource`.
- [x] Invalid source paths throw `IllegalArgumentException` with the bad path in the message, surfaced as `IOException` with a clear "Invalid source configuration: …" message from `startServer()`.
- [x] `GET /v2/search` returns HTTP 503 with a clear JSON error when no sources are open.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
