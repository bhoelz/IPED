# iped-additional-index — Evolution Roadmap

> Module purpose: complementary indexes beyond the main Lucene case index
> (`iped.engine.additionalindex`) — the seam for the 5.0 "additional data stores"
> workstream (graph, vector, time-series).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Extracted as its own engine submodule; currently hosts additional-index support tied to
  the Lucene model.

## Phase 1 — Connector contract
- [ ] Define the pluggable store connector SPI (root roadmap workstream 5): lifecycle
      (open/index/commit/close), item-ID traceability requirement, and consistency policy
      declaration per connector.
- [ ] Rule encoded in tests/docs: the main Lucene index remains the source of truth;
      additional stores are additive and must reference original evidence IDs.

## Phase 2 — First-class connectors
- [ ] Vector store connector for semantic/similarity search (embedding pipeline hooks in
      a task, storage behind the connector).
- [ ] Time-series store connector for temporal analytics (timeline events).
- [ ] Graph store coordination with `iped-engine-graph` (one relationship model, the
      backend chosen by configuration).

## Phase 3 — Operations
- [ ] Rebuild tooling: regenerate any additional index from the authoritative case index
      without reprocessing evidence.
- [ ] Sync/consistency checks reportable via `iped-webapi` (per-store doc counts vs case
      item counts).
- [ ] Distributed-processing support: per-segment additional-index output with a merge or
      late-indexing strategy per connector.

## Progress checks
- A case opened without any additional store configured behaves identically to today.
- Connector contract documented and at least one non-Lucene connector passes an
  end-to-end index→query→trace-back-to-item test.
