# iped-additional-index — Evolution Roadmap

> Module purpose: complementary indexes beyond the main Lucene case index
> (`iped.engine.additionalindex`) — the seam for the 5.0 "additional data stores"
> workstream (graph, vector, time-series).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Extracted as its own engine submodule; currently hosts additional-index support tied to
  the Lucene model.
- Hosts two distinct features: (1) item-level additional task processing (re-run a task on
  selected already-indexed items, store results separately) — see Phase 0, and (2) the 5.0
  "additional data stores" workstream (pluggable vector/graph/time-series connectors) — see
  Phases 1-3, unrelated and much earlier-stage.

## Phase 0 — Item-level additional task processing — `done`
Design doc: [ADDITIONAL_PROCESSING.md](../../iped-engine-parent/iped-additional-index/ADDITIONAL_PROCESSING.md).
- [ISSUE-440](../issues/ISSUE-440-additional-processing-api-contracts.md) — Additional-processing API contracts — `done`
- [ISSUE-441](../issues/ISSUE-441-additional-processing-lucene-storage.md) — Lucene-backed additional-data storage and manager — `done`
- [ISSUE-442](../issues/ISSUE-442-additional-processing-enriched-item-reader.md) — Transparent item enrichment — `done`
- [ISSUE-443](../issues/ISSUE-443-additional-processing-task-runner-worker.md) — Additional-task runner and worker — `done`
- [ISSUE-444](../issues/ISSUE-444-additional-processing-index-task.md) — AdditionalIndexTask terminal stage — `done`
- [ISSUE-445](../issues/ISSUE-445-additional-processing-gui.md) — GUI for selecting items and re-running additional processing — `done`

## Phase 1 — Connector contract — `planned`
- [ISSUE-011](../issues/ISSUE-011-additional-index-connector-spi.md) — Define the pluggable store connector SPI — `done`
- [ISSUE-012](../issues/ISSUE-012-additional-index-lucene-source-of-truth-rule.md) — Encode the Lucene-index-as-source-of-truth rule in tests/docs — `done`

## Phase 2 — First-class connectors — `planned`
- [ISSUE-013](../issues/ISSUE-013-additional-index-vector-store-connector.md) — Vector store connector for semantic/similarity search — `done`
- [ISSUE-014](../issues/ISSUE-014-additional-index-timeseries-store-connector.md) — Time-series store connector for temporal analytics — `done`
- [ISSUE-015](../issues/ISSUE-015-additional-index-graph-store-coordination.md) — Graph store coordination with iped-engine-graph — `planned`

## Phase 3 — Operations — `planned`
- [ISSUE-016](../issues/ISSUE-016-additional-index-rebuild-tooling.md) — Rebuild tooling from the authoritative case index — `planned`
- [ISSUE-017](../issues/ISSUE-017-additional-index-sync-consistency-checks-webapi.md) — Sync/consistency checks reportable via iped-webapi — `planned`
- [ISSUE-018](../issues/ISSUE-018-additional-index-distributed-segment-output.md) — Distributed-processing support for per-segment output — `planned`

## Progress checks
- A case opened without any additional store configured behaves identically to today.
- Connector contract documented and at least one non-Lucene connector passes an
  end-to-end index→query→trace-back-to-item test.
