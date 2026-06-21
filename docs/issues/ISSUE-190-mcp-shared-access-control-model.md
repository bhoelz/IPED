# ISSUE-190: Shared access-control model between iped-mcp and iped-webapi

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 2 — Governance and audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Unify the per-source/per-case permission boundary across the MCP and REST surfaces, so operators configure one consistent set of allowed IDs instead of maintaining separate permission systems.

## Problem

`iped-mcp`'s `--allowed-cases` and `iped-webapi`'s source access control were separate mechanisms, risking inconsistent permission boundaries between the two surfaces.

## Acceptance criteria

- [x] `iped.webapi.allowed-sources` config mirrors `--allowed-cases` semantics.
- [x] Operators can set both to the same IDs to create a unified per-source permission boundary across MCP and REST.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker (completed as part of iped-webapi Phase 6).
