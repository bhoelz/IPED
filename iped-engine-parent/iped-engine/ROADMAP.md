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
- [ ] Move remaining datasource-specific code to `iped-sleuthkit`/`iped-ufed`/`iped-ad1`;
      wire through `iped-api` SPIs.
- [ ] Move task-specific configuration classes to the owning `iped-tasks-*` module
      (pattern already started with the forensics tasks split).
- [ ] ArchUnit rules: engine may not import `iped.app.*`, task-impl, or parser-impl types.
- [ ] Finish logging migration to `@Slf4j`/log4j2 in remaining classes (excluding the
      documented lazy-init logger exceptions).

## Phase 2 — Multi-case hardening
- [ ] Hunt remaining static mutable state under concurrent-case load (known historical
      singletons are done; verify caches, temp-dir handling, native lib init).
- [ ] Crash/resume correctness with multiple active cases (SaveStateThread per-case
      queues under failure injection).
- [ ] Per-case resource quotas validated with large real cases, not just unit tests.

## Phase 3 — Distributed-processing integration
- [ ] Clean seam for `iped-distributed`: engine exposes job/segment lifecycle hooks;
      no Kafka types in engine code.
- [ ] Idempotent, replay-safe item processing (required for Kafka retry/DLQ semantics —
      reprocessing a segment must not duplicate index entries or IDs).
- [ ] Deterministic case merge: distributed segment outputs combine into one case index
      with verifiable counts (ties into `iped-runner` completion detection).

## Phase 4 — 5.0 platform role
- [ ] Engine consumed headless by `iped-webapi`/`iped-mcp`/`iped-runner` as the only entry
      points; Swing-specific hooks (UIPropertyListenerProvider) isolated behind listeners.
- [ ] Performance baseline suite (items/sec, index throughput) recorded per release so
      distributed and multi-case changes can prove no single-case regression.

## Progress checks
- `mvn -pl iped-engine-parent/iped-engine -am verify` green on JDK 25.
- Zero datasource-specific imports; ArchUnit layering tests pass.
- Dual-run validation: same evidence processed monolithic vs distributed yields identical
  item counts and hashes.
