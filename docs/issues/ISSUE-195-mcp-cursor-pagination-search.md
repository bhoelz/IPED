# ISSUE-195: Cursor-based pagination on iped_search

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 3 — Write and job tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Replace offset tracking in `iped_search` with an opaque cursor so AI clients can page through large result sets without needing to compute or remember offsets themselves.

## Problem

Offset-based pagination is error-prone for LLM clients to manage correctly across multi-turn tool calls; an opaque cursor removes that burden.

## Acceptance criteria

- [x] `iped_search` returns a base64-encoded `nextCursor` when more results exist.
- [x] Passing `cursor` fetches the next page without the client tracking offsets.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
