# ISSUE-197: Fix SyncToolSpecification.SyncToolHandler compilation error

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 3 — Write and job tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Fix a pre-existing compilation error caused by referencing a non-existent `SyncToolSpecification.SyncToolHandler` type, by correcting all `spec()` helpers to use the proper functional interface type.

## Problem

`spec()` helpers referenced `SyncToolSpecification.SyncToolHandler`, which does not exist in the MCP SDK, breaking compilation.

## Acceptance criteria

- [x] All `spec()` helpers updated to use `BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult>`.
- [x] Project compiles cleanly.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
