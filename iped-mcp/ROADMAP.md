# iped-mcp — Evolution Roadmap

> Module purpose: MCP server exposing case-safe capabilities to AI clients
> (`iped.engine.mcp.{client,tools}`) — workstream 3 of the root `ROADMAP.md`.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Module scaffolded with tool implementations and a client/DTO layer; consumes engine/
  webapi capabilities.

## Phase 1 — Read-focused MVP (root roadmap Phase B)
- [x] Baseline tool surface complete: `iped_case_list`, `iped_case_open`, `iped_case_get`,
      `iped_case_close`, `iped_search`, `iped_item_get`, `iped_item_preview`,
      `iped_item_related`, `iped_category_list`, full bookmark CRUD (7 tools).
- [x] Route all data access through `iped-webapi` v2 contracts (`WebApiClient` rewritten,
      v2 DTOs `ItemMetadataDto`, `SearchPageDto`).
- [x] Prompt-safe data shaping: `EvidenceGuard` wraps all evidence text in
      `<iped-evidence>` delimiters with 50 000-char cap; binary placeholder for non-text.
- [x] Tool descriptions and JSON schemas reviewed for LLM ergonomics; composable
      `{sourceId}:{docId}` item ID model; pagination on all list/search tools.
- [x] `McpAuditLog` — per-invocation audit trail with arg redaction (strings >200 chars
      truncated); JSONL file persistence when path configured.
- [x] Tests: `WebApiClientTest` (v2 paths), `EvidenceGuardTest`, `McpAuditLogTest`.

## Phase 2 — Governance and audit
- [x] Immutable audit trail (`McpAuditLog`) — JSONL persistence, arg redaction, 100%
      invocation coverage via `ToolRegistry` rate-limit wrapper.
- [x] Tenant/case isolation: `McpSessionContext` + `--allowed-cases=id1,id2` CLI flag;
      `iped_case_list` filters results; `iped_case_get/close/open` reject unlisted cases;
      `CaseAccessDeniedException` returned as tool error.
- [x] Rate limiting: `ToolRateLimiter` fixed-window counter applied cross-cuttingly in
      `ToolRegistry`; configurable via `--rate-limit=N` (default 120 calls/min).
- [x] Request tracing: `McpSessionContext.sessionId` UUID forwarded as
      `X-MCP-Session-Id` on every `WebApiClient` request; `--api-key` forwarded as
      `Authorization: Bearer` for iped-webapi auth.
- [ ] Access-control model shared with `iped-webapi` auth (one permission system) —
      deferred to Phase 4 (requires iped-webapi auth backend).

## Phase 3 — Write and job tools
- [ ] Mutating tools behind explicit capability grants: bookmarks/tags first
      (`item_tag`, `bookmark_create`).
- [ ] `job_create_export` / `job_status` backed by the shared job API
      (`iped-webapi`/`iped-runner` job model).
- [ ] Streaming/pagination for large result tools (cursor tokens, never full dumps).

## Phase 4 — Production (5.0)
- [ ] Transports: stdio for local analyst use + HTTP for service deployment, both with
      the same auth/audit guarantees.
- [ ] Evaluation harness: scripted AI sessions against a golden case verifying tool
      correctness and redaction behavior in CI.
- [ ] Documentation: tool catalog with examples in the scripting/API doc set.

## Progress checks
- An MCP client (e.g., Claude) completes a realistic triage workflow on a golden case
  using only the read toolset.
- Audit log captures 100% of invocations; redaction tests green in CI.
