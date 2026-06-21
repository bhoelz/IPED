# ISSUE-191: Capability grant system for write/job tools

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 3 — Write and job tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Introduce a capability-grant mechanism so write and job-management tools are only registered when an operator explicitly opts in, keeping read-only deployments safe by default.

## Problem

Write-capable tools (bookmarks, jobs) should not be exposed unless explicitly granted, to avoid unintended mutation capability in default deployments.

## Acceptance criteria

- [x] `--capabilities=bookmarks,jobs` CLI flag implemented.
- [x] `GrantedCapabilities` enum added.
- [x] `McpSessionContext.can()` check implemented.
- [x] `ToolRegistry` registers write tools only when granted.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
