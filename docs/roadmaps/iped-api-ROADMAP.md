# iped-api — Evolution Roadmap

> Module purpose: the public contract layer of IPED — interfaces for items, case data,
> datasources, search, tasks, configuration, and I/O that all other modules implement or
> consume. No implementation logic, no heavy dependencies.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Stable interface packages: `iped.data`, `iped.datasource`, `iped.search`, `iped.task`,
  `iped.configuration`, `iped.io`, `iped.properties`, `iped.localization`, `iped.exception`.
- Consumed by every other module; the de facto SPI boundary for parsers, tasks, and viewers.
- No formal versioning/deprecation policy yet; API evolves in lockstep with the engine.

## Phase 1 — Contract hygiene — `done`
- [ISSUE-021](../issues/ISSUE-021-audit-api-engine-leakage.md) — Audit all interfaces for engine-implementation leakage — `done`
- [ISSUE-022](../issues/ISSUE-022-archunit-no-engine-deps.md) — Add ArchUnit test forbidding iped-api dependencies on other modules — `done`
- [ISSUE-023](../issues/ISSUE-023-javadoc-coverage-public-types.md) — Javadoc coverage for every public type in iped-api — `done`
- [ISSUE-024](../issues/ISSUE-024-internal-annotation-for-public-types.md) — Mark internal-but-public types with an `@Internal` annotation — `done`

## Phase 2 — Versioned API for IPED 5.0 — `done`
- [ISSUE-025](../issues/ISSUE-025-semver-deprecation-policy.md) — Define a semantic-versioning and deprecation policy — `done`
- [ISSUE-026](../issues/ISSUE-026-scripting-sdk-stable-surface.md) — Introduce stable surface for the JS/Python scripting SDKs — `done`
- [ISSUE-027](../issues/ISSUE-027-pipeline-event-listener-contracts.md) — Add API-level event/listener contracts for the distributed pipeline — `done`

## Phase 3 — Modularization support — `done`
- [ISSUE-028](../issues/ISSUE-028-datasource-reader-spi.md) — Capability/SPI interfaces for pluggable datasource readers — `done`
- [ISSUE-029](../issues/ISSUE-029-toml-config-schema-contracts.md) — Configuration schema contracts aligned with the TOML config system — `done`
- [ISSUE-030](../issues/ISSUE-030-jpms-module-info.md) — Consider JPMS module-info / explicit Automatic-Module-Name — `done`

## Constraints
- Zero runtime dependencies beyond SLF4J; keep it consumable by external plugin authors.
- Breaking changes require a migration note in `MIGRATION_GUIDE.md`.
