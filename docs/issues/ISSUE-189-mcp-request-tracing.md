# ISSUE-189: Request tracing across MCP and WebApiClient calls

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 2 — Governance and audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Forward a per-session trace identifier and the API key on every downstream `WebApiClient` request, so requests can be correlated across the MCP and webapi layers and authenticated consistently.

## Problem

Without a forwarded session/trace ID, it is difficult to correlate an MCP tool call with the corresponding webapi request for debugging or auditing.

## Acceptance criteria

- [x] `McpSessionContext.sessionId` UUID forwarded as `X-MCP-Session-Id` on every `WebApiClient` request.
- [x] `--api-key` forwarded as `Authorization: Bearer` for iped-webapi auth.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
