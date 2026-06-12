# iped-mcp — Evolution Roadmap

> Module purpose: MCP server exposing case-safe capabilities to AI clients
> (`iped.engine.mcp.{client,tools}`) — workstream 3 of the root `ROADMAP.md`.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Module scaffolded with tool implementations and a client/DTO layer; consumes engine/
  webapi capabilities.

## Phase 1 — Read-focused MVP (root roadmap Phase B)
- [ ] Baseline tool surface complete: `case_list`, `case_open`, `search_query`,
      `search_facets`, `item_get`, `item_content_preview`, `item_relationships`.
- [ ] Route all data access through `iped-webapi` v2 contracts (not engine internals) so
      MCP, web UI, and scripting see identical semantics.
- [ ] Prompt-safe data shaping: size caps, binary-content exclusion, and redaction hooks
      on every tool response (hostile evidence text must not be able to smuggle
      instructions unmarked — wrap evidence content in clearly delimited data blocks).
- [ ] Tool descriptions and JSON schemas reviewed for LLM ergonomics (small, composable
      tools; consistent ID model).

## Phase 2 — Governance and audit
- [ ] Immutable audit trail of every tool invocation (who/what/when/case) — chain-of-
      custody requirement.
- [ ] Tenant/case isolation: an MCP session is bound to authorized cases only.
- [ ] Rate limiting and request tracing per session.
- [ ] Access-control model shared with `iped-webapi` auth (one permission system).

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
