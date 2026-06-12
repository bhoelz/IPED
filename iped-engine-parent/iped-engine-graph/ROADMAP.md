# iped-engine-graph — Evolution Roadmap

> Module purpose: relationship-graph analysis (entity links extracted during processing,
> graph database generation and querying).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Extracted from the monolithic engine into its own module.
- Per-case graph isolation done (GraphService keyed by graph folder per `ARCHITECTURE.md`).

## Phase 1 — Module boundary
- [ ] Depend only on `iped-engine-core` + `iped-api` (verify and enforce with ArchUnit);
      graph generation hooks reach the engine via task/listener SPIs, not direct imports.
- [ ] Make the module optional: a distribution without it must process cases normally
      with graph features disabled (config-gated, clear log message).

## Phase 2 — Storage strategy
- [ ] Evaluate the embedded graph DB choice for 5.0: keep current embedded store vs the
      pluggable graph-store connector planned in root roadmap workstream 5.
- [ ] Define a stable export format for graph data so the storage backend can change
      without reprocessing cases.
- [ ] Distributed-processing compatibility: graph fragments produced per segment must
      merge deterministically (same constraint as the Lucene index).

## Phase 3 — Analysis features
- [ ] Expose graph queries through `iped-webapi` v2 (link expansion, shortest path,
      neighborhood) so the browser UI's Links view doesn't depend on Swing.
- [ ] MCP tool surface for relationship questions (`item_relationships` in the root
      roadmap baseline) backed by this module.

## Progress checks
- Case processed with module absent → success, graph disabled.
- Same case monolithic vs distributed → identical graph node/edge counts.
