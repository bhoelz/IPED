# ISSUE-187: Tenant/case isolation for MCP sessions

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 2 — Governance and audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Restrict an MCP session to an operator-defined allow-list of cases, so multi-tenant deployments cannot leak data across case boundaries.

## Problem

Without case isolation, any MCP client connected to a shared server instance could access any open case, which is unacceptable for multi-tenant forensic deployments.

## Acceptance criteria

- [x] `McpSessionContext` plus `--allowed-cases=id1,id2` CLI flag implemented.
- [x] `iped_case_list` filters results to allowed cases.
- [x] `iped_case_get`/`close`/`open` reject unlisted cases.
- [x] `CaseAccessDeniedException` returned as a tool error for disallowed access.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
