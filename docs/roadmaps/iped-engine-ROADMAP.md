# iped-engine — Evolution Roadmap

> Module purpose: the processing/orchestration engine — Manager, Workers, task pipeline,
> Lucene indexing, evidence processing lifecycle. Depends on all engine-parent siblings.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Multi-case architecture in place per `ARCHITECTURE.md` (CaseContext, ThreadLocal
  delegation, ProcessingOrchestrator, ResourceManager).
- Still contains code that belongs in siblings (datasource leftovers, task-specific
  config — e.g., the `ExportByCategoriesConfig` move left dangling references once).
- Source of truth for case/search semantics; this must stay true through 5.0.

## Phase 1 — Slim down to orchestration — `in_progress`
- [ISSUE-082](../issues/ISSUE-082-move-datasource-code-to-siblings.md) — Move remaining datasource-specific code to sibling modules — `in_progress`
- [ISSUE-083](../issues/ISSUE-083-move-task-config-to-owning-modules.md) — Move task-specific configuration classes to owning iped-tasks-* modules — `in_progress`
- [ISSUE-084](../issues/ISSUE-084-archunit-engine-boundary-rule.md) — ArchUnit rule forbidding engine imports of app/task-impl/parser-impl types — `done`
- [ISSUE-085](../issues/ISSUE-085-finish-logging-migration.md) — Finish logging migration to @Slf4j/log4j2 in remaining classes — `done`

## Phase 2 — Multi-case hardening — `in_progress`
- [ISSUE-086](../issues/ISSUE-086-hunt-static-mutable-state.md) — Hunt remaining static mutable state under concurrent-case load — `done`
- [ISSUE-087](../issues/ISSUE-087-crash-resume-multicase.md) — Crash/resume correctness with multiple active cases — `done`
- [ISSUE-088](../issues/ISSUE-088-per-case-resource-quota-validation.md) — Validate per-case resource quotas with large real cases — `blocked`

## Phase 3 — Distributed-processing integration — `blocked`
- [ISSUE-089](../issues/ISSUE-089-clean-seam-for-distributed.md) — Clean seam for iped-distributed (lifecycle hooks, no Kafka types in engine) — `done`
- [ISSUE-090](../issues/ISSUE-090-idempotent-replay-safe-processing.md) — Idempotent, replay-safe item processing for Kafka retry/DLQ semantics — `done`
- [ISSUE-091](../issues/ISSUE-091-deterministic-case-merge.md) — Deterministic case merge of distributed segment outputs — `blocked`

## Phase 4 — 5.0 platform role — `blocked`
- [ISSUE-092](../issues/ISSUE-092-engine-headless-consumption.md) — Engine consumed headless by iped-webapi/iped-mcp/iped-runner — `done`
- [ISSUE-093](../issues/ISSUE-093-performance-baseline-suite.md) — Performance baseline suite recorded per release — `blocked`

## Progress checks
- `mvn -pl iped-engine-parent/iped-engine -am verify` green on JDK 25.
- Zero datasource-specific imports; ArchUnit layering tests pass.
- Dual-run validation: same evidence processed monolithic vs distributed yields identical
  item counts and hashes.
