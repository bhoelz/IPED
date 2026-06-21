# ISSUE-038: Define companion-app handoff protocol

- Status: done
- Roadmap: [iped-app-ROADMAP.md](../roadmaps/iped-app-ROADMAP.md)
- Roadmap section: Phase 3 — Companion app pivot (5.0 workstream 2)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Define the protocol for handing off items from the browser UI to a native companion app viewer, including a secure local channel and version compatibility checks.

## Problem

As the browser UI becomes the primary analyst interface, items that require native-only viewer capabilities need a secure, well-defined way to be opened in a companion app.

## Acceptance criteria

- [x] Protocol documented in `specs/97-companion-handoff-protocol.md`: loopback HTTP on port 18743, HMAC session token, `POST /open` with `CompanionHandoffRequest` JSON, `GET /version` for protocol check.
- [x] Security constraints specified: loopback-only, token validation, URL allowlist, audit log.
- [x] `CompanionHandoff` interface, `CompanionHandoffRequest` record, and `HttpCompanionHandoff` implementation added to `iped.app.companion` package.
- [x] HTTP probe with 1s cache implemented in `isAvailable()`; `openItem()` returns false on 415/503.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-app-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
