# ISSUE-431: Observability — request tracing across proxy, Jersey, and engine

- Status: done
- Roadmap: [iped-webui-server-ROADMAP.md](../roadmaps/iped-webui-server-ROADMAP.md)
- Roadmap section: Phase 4 — Production posture (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`ProxyTracingFilter` assigns an `X-Trace-Id` per request (reusing an upstream header if present), puts `traceId` in MDC for log correlation, and `ApiProxyController` forwards the header to Jersey; the response carries the same header for browser-console correlation.

## Problem

Diagnosing a request across the proxy, Jersey backend, and engine required a shared trace identifier threaded through logs at every hop.

## Acceptance criteria

- [x] `ProxyTracingFilter` assigns/propagates `X-Trace-Id` per request.
- [x] `traceId` is present in MDC for all log lines.
- [x] `ApiProxyController` forwards the trace header to Jersey; response echoes it back.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webui-server-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
