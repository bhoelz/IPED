# iped-tasks — Evolution Roadmap

> Module purpose: parent of the processing-task modules — `iped-tasks.spi` (task SPI),
> `iped-tasks-bom`, `iped-tasks-core`, feature task modules (transcript, image,
> storage-index, carving, graph, report, forensics), and `iped-tasks-cli` (standalone
> single-task runner, no case/index/DB required).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Task modularization done at the family level; tasks load as plugins via the SPI.
- Task-owned configuration moving here from the engine (e.g., `ExportByCategoriesConfig`
  → `iped-tasks-forensics`) — one known dangling-reference incident shows the move
  needs systematic verification, not just relocation.

## Phase 1 — Finish config/code ownership moves — `done`
- [ISSUE-302](../issues/ISSUE-302-move-imagethumbtaskconfig-to-iped-tasks-image.md) — Move ImageThumbTaskConfig ownership to iped-tasks-image — `done`
- [ISSUE-303](../issues/ISSUE-303-move-videothumbsconfig-to-iped-tasks-image.md) — Move VideoThumbsConfig ownership to iped-tasks-image — `done`
- [ISSUE-304](../issues/ISSUE-304-relocate-htmlreporttaskconfig-class.md) — Relocate the HtmlReportTaskConfig class itself to iped-tasks-report — `done`
- [ISSUE-305](../issues/ISSUE-305-indexsettings-interface-and-findobjectinstanceof.md) — Add IndexSettings interface and findObjectInstanceOf lookup to break the IndexTaskConfig cycle — `done`
- [ISSUE-306](../issues/ISSUE-306-move-indextaskconfig-to-iped-tasks-storage-index.md) — Move IndexTaskConfig to iped-tasks-storage-index and switch engine call sites — `done`
- [ISSUE-307](../issues/ISSUE-307-tasks-spi-and-forensics-archunit-guard.md) — Add ArchUnit guards for iped-tasks.spi and iped-tasks-forensics — `done`

## Phase 2 — SPI maturity — `done`
- [ISSUE-308](../issues/ISSUE-308-document-task-lifecycle-contract.md) — Document the task lifecycle contract in TaskProvider Javadoc — `done`
- [ISSUE-309](../issues/ISSUE-309-document-task-dependency-ordering-spi.md) — Document task dependency/ordering declaration in SPI Javadoc — `done`
- [ISSUE-310](../issues/ISSUE-310-add-taskmetrics-interface.md) — Add TaskMetrics interface to iped-tasks.spi — `done`

## Phase 3 — Distributed and multi-case readiness — `blocked`
- [ISSUE-311](../issues/ISSUE-311-add-taskstatemodel-enum.md) — Add TaskStateModel enum to iped-tasks.spi — `done`
- [ISSUE-312](../issues/ISSUE-312-task-state-classification-matrix-audit.md) — Audit and record the task state-model classification matrix — `done`
- [ISSUE-313](../issues/ISSUE-313-htmlreporttask-case-scoped-state.md) — Move HtmlReportTask static state into a per-case ReportAccumulator — `done`
- [ISSUE-314](../issues/ISSUE-314-graphtask-case-scoped-state.md) — Move GraphTask static state into a per-case GraphAccumulator — `done`
- [ISSUE-315](../issues/ISSUE-315-imagethumbtask-case-scoped-state.md) — Move ImageThumbTask static state into a per-case ImageThumbAccumulator — `done`
- [ISSUE-316](../issues/ISSUE-316-videothumbtask-case-scoped-state.md) — Move VideoThumbTask static state into a per-case VideoThumbAccumulator — `done`
- [ISSUE-317](../issues/ISSUE-317-carvertask-case-scoped-state.md) — Move CarverTask static carverTypes table into a per-case CarverAccumulator — `done`
- [ISSUE-318](../issues/ISSUE-318-basecarvetask-case-scoped-state.md) — Move BaseCarveTask shared carverConfig/ledCarved state into a CarveAccumulator — `done`
- [ISSUE-319](../issues/ISSUE-319-end-of-case-finalization-stage.md) — Add an explicit end-of-case finalization stage for distributed coordination — `blocked`

## Phase 4 — Feature growth (5.0) — `planned`
- [ISSUE-320](../issues/ISSUE-320-transcript-task-pluggable-asr-backends.md) — Transcript task — pluggable ASR backends behind one config surface — `planned`
- [ISSUE-321](../issues/ISSUE-321-image-task-embedding-generation-hook.md) — Image task — embedding generation hook feeding the vector store connector — `planned`
- [ISSUE-322](../issues/ISSUE-322-report-task-align-html-with-browser-ui.md) — Report task — align HTML report output with the browser-UI rendering stack — `planned`

## Phase 5 — Component plugin system (proposal) — `cancelled`
- [ISSUE-447](../issues/ISSUE-447-evaluate-generic-component-plugin-system.md) — Evaluate the generic component plugin system proposal — `cancelled` (no-go; see ADR-0001)

## Phase 6 — Standalone task runner (iped-tasks-cli) hardening — `done`
- [ISSUE-448](../issues/ISSUE-448-standalone-cli-carving-e2e-test.md) — End-to-end standalone CarverTask test through the CLI — `done`
- [ISSUE-449](../issues/ISSUE-449-standalone-imagethumbtask-test.md) — Standalone ImageThumbTask test with a real image — `done`
- [ISSUE-450](../issues/ISSUE-450-taskexecutor-npe-classification-test.md) — Narrow TaskExecutor's NPE-to-UNSUPPORTED classification, add regression test — `done`

## Progress checks
- `mvn -pl iped-tasks -am verify` green; ArchUnit layering green.
- Task classification matrix (stateless/case/global) recorded in this file.
