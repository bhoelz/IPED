# iped-additional-index

Complementary indexes beyond the main Lucene case index (`iped.engine.additionalindex`). This module hosts two distinct features: item-level additional task processing (re-running a task on selected already-indexed items — see [ADDITIONAL_PROCESSING.md](ADDITIONAL_PROCESSING.md) for the design), and the separate, much earlier-stage 5.0 "additional data stores" workstream (graph, vector, time-series connectors).

## Roadmap

See [iped-additional-index-ROADMAP.md](../../docs/roadmaps/iped-additional-index-ROADMAP.md) for planned work, current phase status, and linked issues.

## Design docs

- [ADDITIONAL_PROCESSING.md](ADDITIONAL_PROCESSING.md) — design for item-level additional task processing (`IAdditionalDataSource`, the secondary Lucene store, `AdditionalTaskRunner`). Phases 1-4 of this design are implemented; only the GUI (Phase 5) remains, tracked as ISSUE-445.
