# iped-mcp — Evolution Roadmap

> Module purpose: MCP server exposing case-safe capabilities to AI clients
> (`iped.engine.mcp.{client,tools}`) — workstream 3 of the root `ROADMAP.md`.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Module scaffolded with tool implementations and a client/DTO layer; consumes engine/
  webapi capabilities.

## Phase 1 — Read-focused MVP (root roadmap Phase B) — `done`
- [ISSUE-180](../issues/ISSUE-180-mcp-baseline-tool-surface.md) — Baseline MCP tool surface complete — `done`
- [ISSUE-181](../issues/ISSUE-181-mcp-route-through-webapi-v2.md) — Route all MCP data access through iped-webapi v2 contracts — `done`
- [ISSUE-182](../issues/ISSUE-182-mcp-evidence-guard-prompt-safety.md) — Prompt-safe data shaping with EvidenceGuard — `done`
- [ISSUE-183](../issues/ISSUE-183-mcp-tool-schema-llm-ergonomics.md) — Review tool descriptions and JSON schemas for LLM ergonomics — `done`
- [ISSUE-184](../issues/ISSUE-184-mcp-audit-log.md) — McpAuditLog — per-invocation audit trail with redaction — `done`
- [ISSUE-185](../issues/ISSUE-185-mcp-phase1-test-coverage.md) — Test coverage for read-focused MVP (WebApiClient, EvidenceGuard, McpAuditLog) — `done`

## Phase 2 — Governance and audit — `done`
- [ISSUE-186](../issues/ISSUE-186-mcp-immutable-audit-trail-coverage.md) — Immutable audit trail with 100% invocation coverage — `done`
- [ISSUE-187](../issues/ISSUE-187-mcp-tenant-case-isolation.md) — Tenant/case isolation for MCP sessions — `done`
- [ISSUE-188](../issues/ISSUE-188-mcp-rate-limiting.md) — Rate limiting for MCP tool invocations — `done`
- [ISSUE-189](../issues/ISSUE-189-mcp-request-tracing.md) — Request tracing across MCP and WebApiClient calls — `done`
- [ISSUE-190](../issues/ISSUE-190-mcp-shared-access-control-model.md) — Shared access-control model between iped-mcp and iped-webapi — `done`

## Phase 3 — Write and job tools — `done`
- [ISSUE-191](../issues/ISSUE-191-mcp-capability-grant-system.md) — Capability grant system for write/job tools — `done`
- [ISSUE-192](../issues/ISSUE-192-mcp-item-tag-untag-tools.md) — iped_item_tag / iped_item_untag bookmark-backed tagging tools — `done`
- [ISSUE-193](../issues/ISSUE-193-mcp-bookmark-write-tools-capability-gate.md) — Bookmark write tools gated behind BOOKMARKS capability — `done`
- [ISSUE-194](../issues/ISSUE-194-mcp-job-management-tools.md) — Async job management tools (export/status/cancel) — `done`
- [ISSUE-195](../issues/ISSUE-195-mcp-cursor-pagination-search.md) — Cursor-based pagination on iped_search — `done`
- [ISSUE-196](../issues/ISSUE-196-mcp-webapiclient-write-methods.md) — WebApiClient write methods for tags and jobs — `done`
- [ISSUE-197](../issues/ISSUE-197-mcp-fix-synctoolhandler-compile-error.md) — Fix SyncToolSpecification.SyncToolHandler compilation error — `done`

## Phase 4 — Production (5.0) — `done`
- [ISSUE-198](../issues/ISSUE-198-mcp-stdio-http-transports.md) — stdio and HTTP transports for production deployment — `done`
- [ISSUE-199](../issues/ISSUE-199-mcp-webapiclient-lazy-httpclient-init.md) — WebApiClient lazy HttpClient initialization — `done`
- [ISSUE-200](../issues/ISSUE-200-mcp-evaluation-harness.md) — McpEvalTest evaluation harness for scripted triage workflow — `done`
- [ISSUE-201](../issues/ISSUE-201-mcp-tools-documentation.md) — TOOLS.md documentation for the MCP tool catalog — `done`

## Progress checks
- [x] An MCP client (e.g., Claude) completes a realistic triage workflow on a golden case
      using only the read toolset. (`McpEvalTest.step*` tests green.)
- [x] Audit log captures 100% of invocations; redaction tests green in CI.
      (`McpEvalTest.auditLogCapturesEveryInvocation`, `EvidenceGuardTest` — all pass.)
