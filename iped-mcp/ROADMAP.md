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
- [x] Capability grant system: `--capabilities=bookmarks,jobs` CLI flag; `GrantedCapabilities` enum;
      `McpSessionContext.can()` check; `ToolRegistry` registers write tools only when granted.
- [x] `iped_item_tag` / `iped_item_untag` — bookmark-backed item tagging with auto-create semantics
      (`TagTools`; requires `BOOKMARKS` capability).
- [x] Bookmark write tools (`create`, `delete`, `rename`, `add_items`, `remove_items`) gated behind
      `BOOKMARKS` capability; read tools (`list`, `items`) always available.
- [x] `iped_job_export` / `iped_job_status` / `iped_job_cancel` — async job management backed by
      `POST/GET/DELETE /v2/jobs` (`JobTools`; requires `JOBS` capability). `JobDto` DTO added.
- [x] Cursor-based pagination on `iped_search`: opaque base64-encoded `nextCursor` returned when
      more results exist; pass as `cursor` param to fetch next page without tracking offsets.
- [x] WebApiClient: `tagItem`, `untagItem`, `submitJob`, `getJob`, `cancelJob` methods added.
- [x] Fixed pre-existing compilation error: `SyncToolSpecification.SyncToolHandler` does not exist;
      all `spec()` helpers now use the correct
      `BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult>` type.

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
