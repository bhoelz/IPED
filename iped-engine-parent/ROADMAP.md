# iped-engine-parent — Evolution Roadmap

> Module purpose: aggregator for the engine split — breaking the historical monolithic
> `iped-engine` into focused submodules with enforced dependency boundaries.
> Each submodule has its own `ROADMAP.md`; this file tracks the cross-cutting plan.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

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

## Phase 1 — Complete the split
- [x] Extract `iped-engine-core`, `iped-engine-graph`, `iped-engine-hashdb`.
- [x] Extract datasource modules `iped-sleuthkit`, `iped-ufed`, `iped-ad1`.
- [ ] Sweep remaining datasource-specific code out of `iped-engine` (search for sleuthkit/
      ufed/ad1 imports inside `iped-engine` and invert them through `iped-api` SPIs).
- [ ] Add ArchUnit rules in each submodule encoding the dependency constraints above, so
      regressions fail the build instead of being caught in review.

## Phase 2 — Service-loader datasource registration
- [ ] Replace hardcoded datasource wiring in `iped-engine` with `ServiceLoader`-discovered
      readers registered against an `iped-api` SPI.
- [ ] Make datasource modules optional at runtime: an IPED distribution without
      `iped-sleuthkit` on the classpath should degrade gracefully (clear error, not NPE).

## Phase 3 — Multi-case and distributed maturity
- [ ] Finish de-singletoning per `ARCHITECTURE.md` (CaseContext/ThreadLocal model) for any
      remaining static state discovered under concurrent-case load.
- [ ] Harden the Kafka distributed path (see `iped-distributed/ROADMAP.md`) to
      production-grade per workstream 4 of the root `ROADMAP.md`.

## Phase 4 — 5.0 alignment
- [ ] Engine remains authoritative for case/search semantics; web/MCP layers consume it
      only through `iped-webapi` contracts.
- [ ] Pluggable complementary stores (graph/vector/time-series) integrate via
      `iped-additional-index` connector contracts, never via engine core changes.

## Progress checks
- Reactor build green on JDK 25: `mvn -pl iped-engine-parent -am verify`.
- ArchUnit boundary tests exist and pass in every submodule (Phase 1 exit criterion).
