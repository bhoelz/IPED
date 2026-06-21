# iped-ad1 — Evolution Roadmap

> Module purpose: AccessData AD1 logical-image datasource reader
> (`iped.engine.datasource.ad1`).
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- [x] Extracted from the monolithic engine into its own module.
- Allowed dependencies: `iped-engine-core` + `iped-api` only (within the parent).
- Small, self-contained format decoder — the simplest of the datasource modules and the
  template for how the others should look.

## Phase 1 — Boundary and registration — `planned`
- [ISSUE-001](../issues/ISSUE-001-archunit-rule-ad1-dependency.md) — ArchUnit rule enforcing the dependency constraint — `planned`
- [ISSUE-002](../issues/ISSUE-002-ad1-serviceloader-registration.md) — Register via the ServiceLoader datasource SPI when available — `planned`

## Phase 2 — Robustness — `planned`
- [ISSUE-003](../issues/ISSUE-003-ad1-test-fixtures-decode-regression.md) — Test fixtures with decode regression tests — `planned`
- [ISSUE-004](../issues/ISSUE-004-ad1-truncated-segment-chain-handling.md) — Graceful handling of truncated segment chains — `planned`
- [ISSUE-005](../issues/ISSUE-005-ad1-format-knowledge-documentation.md) — Document AD1 format knowledge in FORMAT.md — `planned`

## Phase 3 — Distributed processing — `planned`
- [ISSUE-006](../issues/ISSUE-006-ad1-entry-range-work-units-kafka.md) — Expose entry-range work units for the Kafka pipeline — `planned`

## Progress checks
- Fixture decode tests green: `mvn -pl iped-engine-parent/iped-ad1 -am test`.
- Engine processes other evidence types with this module absent.
