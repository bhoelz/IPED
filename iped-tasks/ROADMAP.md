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
- [~] `HtmlReportTaskConfig`: only `HTMLReportConfig.toml` moved to `iped-tasks-report`;
      the `.java` class itself was never actually relocated — it still lives in, and
      is owned by, `iped-engine` (`HTMLReportTask` in `iped-tasks-report` resolves it
      via its existing `iped-engine` dependency through a wildcard import). Verified
      this isn't a split-package hazard (only one copy of the class exists), just a
      stale completion claim. Same root blocker as `IndexTaskConfig` below — no
      consumer inside `iped-engine` itself reads it directly here, so once the
      `IndexSettings`-interface pattern is proven out, this one is a pure move with
      no engine-internal callers to redirect.
- [x] `IndexTaskConfig` → `iped-tasks-storage-index` + `IndexTaskConfig.toml` moved.
      `Manager`, `AppAnalyzer`, `ConfiguredFSDirectory`, `QueryBuilder` (all in
      `iped-engine`) couldn't depend on the concrete class without creating a cycle
      (`iped-tasks-storage-index` already depends on `iped-engine`), and the existing
      `ConfigurationManager.findObject(Class)` lookup matches by exact class equality,
      so an interface alone wasn't enough — the engine side would have no concrete
      class to pass as the lookup key. Resolved by: (1) adding `IndexSettings`
      interface to `iped-engine-core` (the 7 accessors the four engine classes
      actually call: `isUseNIOFSDirectory`, `isForceMerge`, `getCommitIntervalSeconds`,
      `getMaxTokenLength`, `isFilterNonLatinChars`, `isConvertCharsToAscii`,
      `isConvertCharsToLowerCase`, `getExtraCharsToIndex`); (2) adding
      `ConfigurationManager.findObjectInstanceOf(Class<I>)` — an `instanceof`-based
      lookup alongside the existing exact-class `findObject`; (3) moving
      `IndexTaskConfig.java` + `IndexTaskConfig.toml` to `iped-tasks-storage-index`,
      implementing `IndexSettings`; (4) switching the four engine call sites to
      `findObjectInstanceOf(IndexSettings.class)`. `iped-app`'s `MenuClass` and
      `iped-tasks-storage-index`'s own `IndexTask`/`ElasticSearchIndexTask` keep
      referencing the concrete `IndexTaskConfig` directly (for `isStoreTermVectors()`,
      not part of the engine-facing interface) — both already depended on
      `iped-tasks-storage-index`, so no cycle there. Verified: `iped-engine-core`,
      `iped-engine`, `iped-tasks-storage-index` test suites green; `iped-app` compiles.
      In the process, found `HtmlReportTaskConfig`'s earlier "moved" entry above was
      inaccurate — see that line for the correction; same root pattern, but no
      engine-internal caller exists for it, so it's a simpler pure move when picked up.
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
- [x] `ImageThumbTask` static state moved into `ImageThumbAccumulator` stored in
      `caseData`: `performanceStatsPerType`, `logInit`, `finished` are now per-case.
      `executor` (thread pool) and `extConvPropInit` (one-time system-property setup)
      left static — they're process-wide resources, not case data.
- [x] `VideoThumbTask` static state moved into `VideoThumbAccumulator` stored in
      `caseData`: `finished`, the six processing counters (`totalVideosProcessed/
      Failed/Time`, `totalAnimatedImagesProcessed/Failed/Time`, `totalTimeGallery`,
      `totalGallery`), and the `processedVideos` reuse cache are now per-case.
      `taskEnabled`/`mplayer`/`init` left static — one-time MPlayer detection,
      not case data.
- [x] `CarverTask` static `carverTypes[]` moved into `CarverAccumulator` stored in
      `caseData` — previously case 2's signature table in a batch run could be
      silently skipped because the static field was already non-null from case 1.
      `carverConfig`/`ledCarved`/`itensCarved` remain static in `BaseCarveTask`
      (iped-engine, shared with `LedCarveTask`/`KnownMetCarveTask`) — out of scope
      here since fixing them requires touching all three carve task subclasses and
      a cross-module (`iped-engine`) change; tracked as a follow-up.
- [x] `BaseCarveTask` (`iped-engine`) case-scoped state move: `carverConfig` and
      `ledCarved` moved into a `CarveAccumulator` stored in `caseData`, accessed via
      `getCarverConfig()`/`setCarverConfig()`/`ledCarved()` — shared correctly across
      `CarverTask`/`LedCarveTask`/`KnownMetCarveTask` since the accumulator lives on
      the base class. Previously case 2 could silently reuse case 1's carver config
      and `ledCarved` offsets in a batch/report run. `itensCarved` left static per
      the per-field review: `getItensCarved()` is read externally (UI progress,
      `Statistics` log) as an intentional cumulative run counter, not case data.
      `LedCarveTask`'s own counters (`numCarvedItems`, `bytesHashed`, `num512total`,
      `num512hit`, `finished`) moved into a `LedCarveAccumulator`; `ledHashDB`/
      `hashDBDataSource`/`taskEnabled`/`init` left static (one-time hash DB load,
      doesn't vary across cases in the same batch — same rationale as
      `VideoThumbTask`'s MPlayer detection). `KnownMetCarveTask`'s `numCarvedItems`/
      `finished` moved into a `KnownMetAccumulator` the same way.
      `ignoreCorrupted` in `CarverTask` was assessed and left static: `ParsingTask`
      (a different module/family) reads it directly as `CarverTask.ignoreCorrupted`;
      case-scoping it would require touching that external read too — left as a
      known follow-up rather than expanding this change's blast radius.
- [ ] Tasks with end-of-case phases (graph generation, report, storage-index) get an
      explicit "finalization" stage the distributed coordinator can invoke once after
      all segments merge. **Investigated, not yet buildable**: `CaseLifecycleManager.
      completeCase()` (`iped-distributed`) only flips a status flag and persists —
      there is no cross-node merge machinery yet (nothing combines graph CSVs, report
      HTML, or carved-item lists produced by separate nodes). `GraphTask`/
      `HTMLReportTask`/`CarverTask` also still extend the legacy `AbstractTask`, not
      the newer `TaskProvider` SPI, so a `finalizeCase()` hook added to that SPI today
      would have no caller. Concrete prerequisite before this item is actionable:
      (1) build the actual segment-merge step in `iped-distributed` (what gets merged
      and how, per task family), (2) migrate `GraphTask`/`HTMLReportTask`/`CarverTask`
      onto `TaskProvider`, (3) only then add the finalization hook the merge step
      invokes. Each of those is its own scoped task.

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
