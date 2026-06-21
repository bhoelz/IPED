# ISSUE-199: WebApiClient lazy HttpClient initialization

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 4 — Production (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Defer `WebApiClient`'s `HttpClient` socket creation until the first actual call, so tests can stub the client in-process without requiring a real loopback connection.

## Problem

Eager `HttpClient` construction forced tests to depend on actual network/loopback availability even when using in-process stubs.

## Acceptance criteria

- [x] `HttpClient` initialization deferred to first actual call.
- [x] In-process test stubs work without loopback dependencies.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
