# iped-tasks — Evolution Roadmap

> Module purpose: parent of the processing-task modules — `iped-tasks.spi` (task SPI),
> `iped-tasks-bom`, `iped-tasks-core`, and feature task modules (transcript, image,
> storage-index, carving, graph, report, forensics).
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Task modularization done at the family level; tasks load as plugins via the SPI.
- Task-owned configuration moving here from the engine (e.g., `ExportByCategoriesConfig`
  → `iped-tasks-forensics`) — one known dangling-reference incident shows the move
  needs systematic verification, not just relocation.

## Phase 1 — Finish config/code ownership moves
- [ ] Complete the migration of task-specific config classes out of `iped-engine` into
      the owning task module, with a full-reactor compile check per move.
- [ ] Each task module owns its TOML config defaults on the classpath (established
      convention); engine sees only the generic enable/disable property mechanism.
- [ ] ArchUnit at parent level: task modules depend on `iped-tasks.spi`/`core` +
      `iped-api`; never on `iped-app` or engine internals beyond the SPI.

## Phase 2 — SPI maturity
- [ ] Document the task lifecycle contract (`init/process/finish`, per-worker instances,
      thread-safety expectations) in `iped-tasks.spi` — this is the third-party
      extension point alongside parsers.
- [ ] Task dependency/ordering declaration in the SPI (today: implicit pipeline order)
      so plugin tasks can insert themselves safely.
- [ ] Per-task metrics (items/sec, errors, queue time) emitted uniformly for the
      observability stack.

## Phase 3 — Distributed and multi-case readiness
- [ ] Classify every task: stateless per-item, case-scoped state, or global state —
      prerequisite for running tasks on distributed agents.
- [ ] Case-scoped state moves into `CaseContext`-aware holders (no statics) for
      multi-case correctness.
- [ ] Tasks with end-of-case phases (graph generation, report, storage-index) get an
      explicit "finalization" stage the distributed coordinator can invoke once after
      all segments merge.

## Phase 4 — Feature growth (5.0)
- [ ] Transcript task: pluggable ASR backends (local model vs remote service) behind one
      config surface.
- [ ] Image task: embedding generation hook feeding the vector store connector
      (`iped-additional-index`).
- [ ] Report task: HTML report output aligned with the browser-UI rendering stack
      instead of legacy static templates.

## Progress checks
- `mvn -pl iped-tasks -am verify` green; ArchUnit layering green.
- Task classification matrix (stateless/case/global) recorded in this file.
