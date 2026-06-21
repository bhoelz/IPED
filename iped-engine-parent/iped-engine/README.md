# iped-engine

The processing/orchestration engine — Manager, Workers, task pipeline, Lucene indexing, evidence processing lifecycle. Depends on all `iped-engine-parent` siblings and is the source of truth for case/search semantics.

## Roadmap

See [iped-engine-ROADMAP.md](../../docs/roadmaps/iped-engine-ROADMAP.md) for planned work, current phase status, and linked issues. Two related sub-roadmaps also live there: [iped-engine-plugin-registry-ROADMAP.md](../../docs/roadmaps/iped-engine-plugin-registry-ROADMAP.md) (GitHub-based plugin registry) and [iped-engine-schemas-ROADMAP.md](../../docs/roadmaps/iped-engine-schemas-ROADMAP.md) (JSON/UI schema generation for configurable components).

## Further reading

- [ARCHITECTURE.md](ARCHITECTURE.md) — multi-case processing architecture (CaseContext, ProcessingOrchestrator, per-case isolation), corresponds to the now-`done` Phase 2 of the roadmap above.
- [MULTI-CASE-MIGRATION-GUIDE.md](MULTI-CASE-MIGRATION-GUIDE.md) — API guide for task/parser developers and application integrators working with multi-case processing.
- [MULTI-CASE-QUICK-START.md](MULTI-CASE-QUICK-START.md) — minimal example to submit and monitor a case.
- [PROCESSING_FLOW.md](PROCESSING_FLOW.md) — end-to-end single-case data flow, from datasource intake to persistence.
