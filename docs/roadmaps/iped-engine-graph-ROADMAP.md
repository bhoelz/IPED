# iped-engine-graph — Evolution Roadmap

> Module purpose: relationship-graph analysis (entity links extracted during processing,
> graph database generation and querying).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Extracted from the monolithic engine into its own module.
- Per-case graph isolation done (GraphService keyed by graph folder per `ARCHITECTURE.md`).

## Phase 1 — Module boundary — `planned`
- [ISSUE-108](../issues/ISSUE-108-graph-module-boundary-archunit.md) — Enforce module boundary via ArchUnit and SPI hooks — `planned`
- [ISSUE-109](../issues/ISSUE-109-graph-module-optional-config-gated.md) — Make the module fully optional via config gating — `planned`

## Phase 2 — Storage strategy — `planned`
- [ISSUE-110](../issues/ISSUE-110-graph-embedded-db-storage-evaluation.md) — Evaluate embedded graph DB choice for 5.0 — `planned`
- [ISSUE-111](../issues/ISSUE-111-graph-stable-export-format.md) — Define a stable export format for graph data — `planned`
- [ISSUE-112](../issues/ISSUE-112-graph-distributed-fragment-merge.md) — Deterministic merge of per-segment graph fragments — `planned`

## Phase 3 — Analysis features — `planned`
- [ISSUE-113](../issues/ISSUE-113-graph-webapi-v2-query-surface.md) — Expose graph queries through iped-webapi v2 — `planned`
- [ISSUE-114](../issues/ISSUE-114-graph-mcp-item-relationships-tool.md) — MCP tool surface for relationship questions — `planned`

## Progress checks
- Case processed with module absent → success, graph disabled.
- Same case monolithic vs distributed → identical graph node/edge counts.
