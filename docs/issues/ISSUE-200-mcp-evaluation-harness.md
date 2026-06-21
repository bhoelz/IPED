# ISSUE-200: McpEvalTest evaluation harness for scripted triage workflow

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 4 — Production (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Build an end-to-end evaluation harness that exercises a scripted triage workflow against an in-memory webapi stub, covering tool surface completeness, evidence wrapping, pagination, write/job round-trips, audit coverage, and error paths.

## Problem

The MCP server lacked an automated way to verify that a realistic multi-step triage workflow works correctly end to end without depending on a live HTTP backend.

## Acceptance criteria

- [x] `McpEvalTest` runs against `WebApiStub` (in-memory, no HTTP).
- [x] Covers tool-surface completeness (21 tools with full caps, 11 read-only).
- [x] Covers EvidenceGuard wrapping on all previews.
- [x] Covers 3-page cursor navigation.
- [x] Covers tag/job/cancel round-trips.
- [x] Covers audit-log call coverage.
- [x] Covers invalid-cursor error path.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
