# ISSUE-185: Test coverage for read-focused MVP (WebApiClient, EvidenceGuard, McpAuditLog)

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 1 — Read-focused MVP (root roadmap Phase B)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add test coverage for the core read-MVP building blocks: the v2-aware web API client, evidence guard wrapping, and the audit log.

## Problem

The new v2-routed client, evidence-guard wrapping, and audit logging needed regression coverage before being relied on by tool implementations.

## Acceptance criteria

- [x] `WebApiClientTest` covers v2 paths.
- [x] `EvidenceGuardTest` added.
- [x] `McpAuditLogTest` added.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
