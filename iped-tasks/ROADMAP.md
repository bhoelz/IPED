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
- [x] `ImageThumbTaskConfig` → `iped-tasks-image` + `ImageThumbsConfig.toml` moved.
- [x] `VideoThumbsConfig` → `iped-tasks-image` + `VideoThumbsConfig.toml` moved.
- [x] `HtmlReportTaskConfig` → `iped-tasks-report` + `HTMLReportConfig.toml` moved.
- [~] `IndexTaskConfig` — deferred: `ConfiguredFSDirectory`, `AppAnalyzer`, and
      `QueryBuilder` in `iped-engine` read this config directly. Moving it to
      `iped-tasks-storage-index` would create a circular dependency. Resolution:
      extract an `IndexSettings` interface to `iped-engine-core` or `iped-api`,
      implement in `iped-tasks-storage-index`, and have engine internals depend on
      the interface only.
- [x] ArchUnit guard added to `iped-tasks.spi` (SPI may only import `iped-api`)
      and `iped-tasks-forensics` (task code may not import `iped.app.*`).

## Phase 2 — SPI maturity
- [x] Document the task lifecycle contract (`init/process/finish`, per-worker instances,
      thread-safety expectations) in `TaskProvider` Javadoc — complete contract reference
      for third-party implementors.
- [x] Task dependency/ordering declaration documented in SPI Javadoc with `TaskDependency`
      factory methods (`requires`, `before`, `after`).
- [x] `TaskMetrics` interface added to `iped-tasks.spi` — tasks that implement it expose
      `processedCount`, `errorCount`, `processingMillis`, and `itemsPerSecond()`; the
      engine discovers it via `instanceof` on the task object.

## Phase 3 — Distributed and multi-case readiness
- [x] `TaskStateModel` enum added to `iped-tasks.spi` with three values: `STATELESS`,
      `CASE_SCOPED`, `GLOBAL` (default). `TaskProvider.stateModel()` default method
      returns `GLOBAL`; providers override to declare a safer model.
- [x] Task classification matrix (from code audit):

  | Task | State model | Key issue |
  |------|-------------|-----------|
  | HashTask | **STATELESS** | MessageDigest reset per item; executor is stateless |
  | ExportFileTask | CASE_SCOPED (mostly) | Output maps keyed by case dir; `noContentHashes` still global |
  | ImageThumbTask | **GLOBAL** | `performanceStatsPerType` static map never cleared between cases |
  | VideoThumbTask | **GLOBAL** | Static counters + `processedVideos` cache not reset between cases |
  | CarverTask | **GLOBAL** | Static `carverTypes[]`, `ledCarved` shared map |
  | KnownMetCarveTask | **GLOBAL** | `numCarvedItems` counter not reset; inherited `ledCarved` |
  | LedCarveTask | **GLOBAL** | Static `ledHashDB` and counters not case-scoped |
  | HtmlReportTask | **GLOBAL** | `entriesByLabel/Category/NoLabel` static collections never cleared |
  | GraphTask | **GLOBAL** | `formattedPhonesCache` and `datasourceOwnerMap` static |

- [x] `HtmlReportTask` static state moved into `ReportAccumulator` stored in `caseData`:
      `entriesByLabel`, `entriesByCategory`, `entriesNoLabel`, `imageThumbsByLabel`,
      `currentFiles`, `reportSubFolder`, `externalImageConverter`, and `init`
      (AtomicBoolean) are now per-case fields. Double-checked locking via
      `synchronized(HTMLReportTask.class)` creates exactly one `ReportAccumulator`
      per `caseData` instance. `getReportSubFolder()` changed to instance method.
- [x] `GraphTask` static state moved into `GraphAccumulator` stored in `caseData`:
      `graphFileWriter` and `datasourceOwnerMap` are now per-case fields.
      `formattedPhonesCache` left as static (bounded LRU perf cache, cross-case reuse
      is intentional). `commit()` kept static (reflection call from Manager) via
      `volatile static activeAccumulator` forwarding reference set in `accum()` and
      cleared in `finish()`. `synchronized(this.getClass())` in `getGenericOwnerNode`
      replaced with `synchronized(accum())` for per-case isolation.
- [ ] Remaining case-scoped state moves (ImageThumbTask, VideoThumbTask, CarverTask)
      into `CaseContext`-aware holders — `HtmlReportTask`/`GraphTask` pattern established.
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
