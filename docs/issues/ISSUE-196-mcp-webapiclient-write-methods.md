# ISSUE-196: WebApiClient write methods for tags and jobs

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 3 — Write and job tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add the underlying `WebApiClient` methods needed to back the new tag and job tools.

## Problem

The tag and job MCP tools needed corresponding HTTP client methods to call the webapi's v2 endpoints.

## Acceptance criteria

- [x] `tagItem`, `untagItem` methods added to `WebApiClient`.
- [x] `submitJob`, `getJob`, `cancelJob` methods added to `WebApiClient`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
