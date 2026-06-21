# ISSUE-184: McpAuditLog — per-invocation audit trail with redaction

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 1 — Read-focused MVP (root roadmap Phase B)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add a per-invocation audit trail for MCP tool calls, redacting long argument strings and optionally persisting to a JSONL file.

## Problem

Without an audit trail, there is no record of what tools an AI client invoked against a case, which is a compliance concern in a forensics context.

## Acceptance criteria

- [x] `McpAuditLog` records every tool invocation.
- [x] Arg redaction applied: strings longer than 200 chars truncated.
- [x] JSONL file persistence available when a path is configured.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
