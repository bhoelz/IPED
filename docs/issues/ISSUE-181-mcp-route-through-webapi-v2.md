# ISSUE-181: Route all MCP data access through iped-webapi v2 contracts

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 1 — Read-focused MVP (root roadmap Phase B)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Rewrite the MCP server's data access layer so all evidence access flows through `iped-webapi`'s v2 contracts rather than ad-hoc or v1 paths, keeping a single backend contract.

## Problem

Without consistent routing through v2 contracts, the MCP server risked diverging from the canonical API surface used by other clients.

## Acceptance criteria

- [x] `WebApiClient` rewritten to use v2 endpoints.
- [x] v2 DTOs `ItemMetadataDto` and `SearchPageDto` added.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
