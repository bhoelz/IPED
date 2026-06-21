# ISSUE-186: Immutable audit trail with 100% invocation coverage

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 2 — Governance and audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Guarantee that the audit trail captures every tool invocation by enforcing it cross-cuttingly via the tool registry's rate-limit wrapper, rather than relying on each tool to log itself.

## Problem

Per-tool audit logging is easy to miss for new tools; a cross-cutting guarantee is needed to ensure no invocation goes unrecorded.

## Acceptance criteria

- [x] `McpAuditLog` persists to JSONL with arg redaction.
- [x] 100% invocation coverage enforced via the `ToolRegistry` rate-limit wrapper.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
