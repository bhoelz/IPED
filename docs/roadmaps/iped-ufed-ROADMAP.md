# iped-ufed — Evolution Roadmap

> Module purpose: UFED/Cellebrite datasource reader — parses UFDR/UFED XML reports and
> maps extracted mobile artifacts into the IPED item model.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- [x] Extracted from the monolithic engine into its own module
      (`iped.engine.datasource.ufed`).
- Allowed dependencies: `iped-engine-core` + `iped-api` only (within the parent).

## Phase 1 — Boundary and registration — `planned`
- [ISSUE-325](../issues/ISSUE-325-ufed-archunit-boundary.md) — ArchUnit rule enforcing the dependency constraint — `planned`
- [ISSUE-326](../issues/ISSUE-326-ufed-serviceloader-registration.md) — Register via the ServiceLoader datasource SPI — `planned`
- [ISSUE-327](../issues/ISSUE-327-ufed-parser-coupling-shared-artifact-model.md) — Untangle parser-side UFED coupling via a shared artifact model — `planned`

## Phase 2 — Format coverage — `planned`
- [ISSUE-328](../issues/ISSUE-328-ufed-version-matrix-regression-fixtures.md) — Version matrix and regression fixtures for export versions — `planned`
- [ISSUE-329](../issues/ISSUE-329-ufed-streaming-xml-parse-large-reports.md) — Streaming XML parse for very large UFDR reports — `planned`
- [ISSUE-330](../issues/ISSUE-330-ufed-unknown-element-telemetry.md) — Unknown-element telemetry for artifact mapping — `planned`

## Phase 3 — Distributed processing — `planned`
- [ISSUE-331](../issues/ISSUE-331-ufed-distributed-work-unit-strategy.md) — Decide UFED distributed work-unit strategy — `planned`

## Progress checks
- Fixture suite covering at least the 3 most common UFED export versions, green in CI.
- Engine processes non-UFED evidence with this module absent.
