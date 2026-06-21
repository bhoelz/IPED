# ISSUE-188: Rate limiting for MCP tool invocations

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 2 — Governance and audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Apply a configurable fixed-window rate limit across all MCP tool calls to protect the server and underlying engine from runaway or abusive clients.

## Problem

Without rate limiting, a misbehaving or malicious AI client could overwhelm the MCP server (and the engine behind it) with excessive tool calls.

## Acceptance criteria

- [x] `ToolRateLimiter` fixed-window counter applied cross-cuttingly in `ToolRegistry`.
- [x] Configurable via `--rate-limit=N` (default 120 calls/min).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
