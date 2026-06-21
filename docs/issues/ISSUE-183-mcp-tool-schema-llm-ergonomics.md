# ISSUE-183: Review tool descriptions and JSON schemas for LLM ergonomics

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 1 — Read-focused MVP (root roadmap Phase B)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Review and refine MCP tool descriptions and JSON schemas so LLM clients can use them reliably, including a composable item ID model and consistent pagination.

## Problem

Poorly designed tool descriptions/schemas lead to LLM clients misusing tools or constructing invalid requests.

## Acceptance criteria

- [x] Tool descriptions and JSON schemas reviewed for LLM ergonomics.
- [x] Composable `{sourceId}:{docId}` item ID model adopted.
- [x] Pagination present on all list/search tools.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
