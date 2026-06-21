# ISSUE-239: AuthN/authZ for the runner API

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 3 — Operations
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`RunnerApiKeyFilter` enforces API-key auth on the runner API, accepting `X-Api-Key` or `Authorization: Bearer`, while remaining a no-op in open-access dev mode when no key is configured.

## Problem

The runner API had no authentication, which is unacceptable once it is exposed beyond a trusted local network.

## Acceptance criteria

- [x] `RunnerApiKeyFilter` validates `runner.api-key` against `X-Api-Key` or `Authorization: Bearer`.
- [x] Filter is a no-op when `runner.api-key` is blank.
- [x] Dashboard SSE, browse, and actuator endpoints remain accessible without a key.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
