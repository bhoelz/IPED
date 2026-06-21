# iped-engine-parent — Evolution Roadmap

> Module purpose: aggregator for the engine split — breaking the historical monolithic
> `iped-engine` into focused submodules with enforced dependency boundaries.
> Each submodule has its own `ROADMAP.md`; this file tracks the cross-cutting plan.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Submodules
| Module | Role |
|---|---|
| `iped-engine-core` | Shared engine abstractions: config registry/schema, data model, search, IO. No datasource-specific code. |
| `iped-engine` | Orchestration: Manager, Workers, indexing, processing pipeline. Depends on all siblings. |
| `iped-engine-graph` | Graph analysis (Neo4j-backed relationship graph). |
| `iped-engine-hashdb` | Hash databases (LedDB, PhotoDNA, NSRL-style lookups). |
| `iped-sleuthkit` | Sleuthkit datasource reader (disk images). |
| `iped-ufed` | UFED/Cellebrite XML datasource reader. |
| `iped-ad1` | AccessData AD1 datasource reader. |
| `iped-distributed` | Kafka-based distributed processing (coordinator, agent, status). |
| `iped-additional-index` | Complementary index support beyond the main Lucene index. |

## Architectural rules (target end-state)
- Datasource modules (`iped-sleuthkit`, `iped-ufed`, `iped-ad1`) depend **only** on
  `iped-engine-core` within the parent; `iped-engine` depends on all of them.
- `iped-engine-core` has no dependency on any sibling.
- `iped-distributed` depends on `iped-engine` (it drives processing) but engine code must
  never depend on Kafka types.

## Phase 1 — Complete the split — `in_progress`
- [ISSUE-128](../issues/ISSUE-128-extract-engine-core-graph-hashdb.md) — Extract iped-engine-core, iped-engine-graph, iped-engine-hashdb — `done`
- [ISSUE-129](../issues/ISSUE-129-extract-datasource-modules.md) — Extract datasource modules iped-sleuthkit, iped-ufed, iped-ad1 — `done`
- [ISSUE-130](../issues/ISSUE-130-sweep-datasource-imports-from-engine.md) — Sweep remaining datasource-specific code out of iped-engine — `planned`
- [ISSUE-131](../issues/ISSUE-131-archunit-rules-per-submodule.md) — Add ArchUnit rules encoding dependency constraints in each submodule — `planned`

## Phase 2 — Service-loader datasource registration — `planned`
- [ISSUE-132](../issues/ISSUE-132-serviceloader-datasource-wiring.md) — Replace hardcoded datasource wiring with ServiceLoader-discovered readers — `planned`
- [ISSUE-133](../issues/ISSUE-133-optional-datasource-modules-at-runtime.md) — Make datasource modules optional at runtime with graceful degradation — `planned`

## Phase 3 — Multi-case and distributed maturity — `in_progress`
- [ISSUE-134](../issues/ISSUE-134-finish-desingletoning-static-state.md) — Finish de-singletoning remaining static state under concurrent-case load — `planned`
- [ISSUE-135](../issues/ISSUE-135-harden-kafka-distributed-path.md) — Harden the Kafka distributed path to production-grade — `in_progress`

## Phase 4 — 5.0 alignment — `planned`
- [ISSUE-136](../issues/ISSUE-136-engine-authoritative-via-webapi.md) — Engine remains authoritative for case/search semantics via iped-webapi contracts — `planned`
- [ISSUE-137](../issues/ISSUE-137-pluggable-complementary-stores.md) — Pluggable complementary stores integrate via iped-additional-index connectors — `planned`

## Progress checks
- Reactor build green on JDK 25: `mvn -pl iped-engine-parent -am verify`.
- ArchUnit boundary tests exist and pass in every submodule (Phase 1 exit criterion).
