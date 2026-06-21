# ISSUE-387: Close deferred iped-mcp shared access-control item

- Status: done
- Roadmap: [iped-webapi-ROADMAP.md](../roadmaps/iped-webapi-ROADMAP.md)
- Roadmap section: Phase 6 — Per-source ACL and item write endpoints
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Close out the deferred iped-mcp Phase 2 roadmap item "Access-control model shared with iped-webapi auth" now that `iped.webapi.allowed-sources` exists as the engine-side complement to MCP's `--allowed-cases`.

## Problem

`iped-mcp` had a deferred roadmap item waiting on `iped-webapi` to provide a matching source-level access control mechanism before a unified permission boundary could be declared complete.

## Acceptance criteria

- [x] Confirm `iped.webapi.allowed-sources` (enforced by `SourceAccessFilter`) is the engine-side complement to `iped-mcp`'s `--allowed-cases`.
- [x] Document that an operator deploys both with consistent IDs to create a unified per-source permission boundary across the MCP and REST surfaces.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-webapi-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
