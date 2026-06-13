# iped-engine — Evolution Roadmap

> Module purpose: the processing/orchestration engine — Manager, Workers, task pipeline,
> Lucene indexing, evidence processing lifecycle. Depends on all engine-parent siblings.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Multi-case architecture in place per `ARCHITECTURE.md` (CaseContext, ThreadLocal
  delegation, ProcessingOrchestrator, ResourceManager).
- Still contains code that belongs in siblings (datasource leftovers, task-specific
  config — e.g., the `ExportByCategoriesConfig` move left dangling references once).
- Source of truth for case/search semantics; this must stay true through 5.0.

## Phase 1 — Slim down to orchestration
- [~] Move remaining datasource-specific code to `iped-sleuthkit`/`iped-ufed`/`iped-ad1`;
      wire through `iped-api` SPIs.
      (SPI contract defined: `iped.datasource.spi.IDataSourceReader` in iped-api with
      ServiceLoader registration. Physical code move deferred — requires coordinated changes
      to `iped-sleuthkit`/`iped-ufed`/`iped-ad1` modules outside this branch's scope.)
- [~] Move task-specific configuration classes to the owning `iped-tasks-*` module
      (pattern already started with the forensics tasks split).
      (Deferred — requires coordinated changes across `iped-tasks-*` modules.
      Typed config access via `ITypedConfigAccess` already removes the hard dependency.)
- [x] ArchUnit rules: engine may not import `iped.app.*`, task-impl, or parser-impl types.
      (`EngineBoundaryTest` added — checks no Swing, no Kafka, no iped.app imports from
      engine orchestration/config/task/lucene packages.)
- [x] Finish logging migration to `@Slf4j`/log4j2 in remaining classes (excluding the
      documented lazy-init logger exceptions).
      (`IPEDCrawler`: 12+ `System.out/err.println` → `log.info/error`;
      `CmdLineArgsImpl`: 2 `System.out.println` → `log.info/error`;
      `ProcessUtil`: `System.err.println` + `printStackTrace` → `log.error(..., e)`;
      `CustomIndexDeletionPolicy`: 2 `System.out.println` → `log.info`;
      Unused `java.util.logging` imports removed from `IPEDReader`.)

## Phase 2 — Multi-case hardening
- [x] Hunt remaining static mutable state under concurrent-case load.
      Fixed: `Worker.workerNamePrefix` → `final`; `Manager.commitIntervalMillis` → instance field;
      `EvidenceStatus` path constants → `final`; `QueuesProcessingOrder.mediaTypes` →
      `ConcurrentHashMap` + `mediaRegistry` → `volatile`; `IPEDMultiSource.baseDocCache` →
      instance field (was `static ArrayList`, risked cross-case data corruption);
      `Item.extraAttributeSet` → `final` (was reassignable).
- [x] Crash/resume correctness with multiple active cases (SaveStateThread per-case
      queues under failure injection).
      (Audit confirmed `SaveStateThread` already uses `ConcurrentHashMap<UUID, Queue<...>>`
      keyed by case UUID — per-case state is correctly isolated. No code change required.)
- [~] Per-case resource quotas validated with large real cases, not just unit tests.
      (`ResourceManager` is per-case. Integration tests with real multi-GB evidence require
      dedicated lab infrastructure; deferred to a follow-up task outside this branch.)

## Phase 3 — Distributed-processing integration
- [x] Clean seam for `iped-distributed`: engine exposes job/segment lifecycle hooks;
      no Kafka types in engine code. (`iped.engine.pipeline.EngineHooks` added;
      `CaseContext.getHooks()` exposes it; `iped-distributed` registers listeners via
      `IJobLifecycleListener` / `IItemProcessingListener` from iped-api.)
- [x] Idempotent, replay-safe item processing (required for Kafka retry/DLQ semantics —
      reprocessing a segment must not duplicate index entries or IDs).
      (`CaseContext.processedTrackIds` (`ConcurrentHashMap.newKeySet()`) added.
      `Worker.processNewItem` checks `processedTrackIds.add(trackId)` before queuing;
      duplicate items are logged at DEBUG and skipped. Queue-end sentinels bypass the check.)
- [~] Deterministic case merge: distributed segment outputs combine into one case index
      with verifiable counts (ties into `iped-runner` completion detection).
      (Deferred — requires `iped-runner` and distributed segment-output infrastructure
      outside this branch's scope.)

## Phase 4 — 5.0 platform role
- [x] Engine consumed headless by `iped-webapi`/`iped-mcp`/`iped-runner` as the only entry
      points; Swing-specific hooks (UIPropertyListenerProvider) isolated behind listeners.
      (`UIPropertyListenerProvider.uiDispatcher` defaults to `Runnable::run` (headless-safe);
      iped-app overrides it with `SwingUtilities::invokeLater`. Zero `javax.swing` imports
      in engine code confirmed by grep. `java.awt` usages in `IPEDReader`, `Bookmarks`, and
      `ImageSimilarity` are image-processing APIs safe with `-Djava.awt.headless=true`.
      `EngineBoundaryTest` enforces no Swing in core/config/lucene/task packages.)
- [~] Performance baseline suite (items/sec, index throughput) recorded per release so
      distributed and multi-case changes can prove no single-case regression.
      (Deferred — requires dedicated benchmark infrastructure and representative evidence sets.
      JMH dependency and skeleton benchmark class to be added in a follow-up task.)

## Progress checks
- `mvn -pl iped-engine-parent/iped-engine -am verify` green on JDK 25.
- Zero datasource-specific imports; ArchUnit layering tests pass.
- Dual-run validation: same evidence processed monolithic vs distributed yields identical
  item counts and hashes.
