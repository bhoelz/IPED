# iped-engine-core — Evolution Roadmap

> Module purpose: shared engine abstractions — configuration registry/schema, core data
> model, datasource SPI, search primitives, IO, localization. Contains **no**
> datasource-specific or orchestration code.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state (2026-06)
- Hosts the TOML configuration system (`iped.engine.config.{api,registry,schema}`) after
  the migration away from `.txt` configs.
- Sits below the datasource modules and `iped-engine` in the dependency graph.

## Phase 1 — Boundary enforcement — `done`
- [ISSUE-096](../issues/ISSUE-096-archunit-no-sibling-dependency.md) — ArchUnit rule forbidding dependency on iped-engine-parent siblings — `done`
- [ISSUE-097](../issues/ISSUE-097-audit-orchestration-leakage.md) — Audit for orchestration logic leaked from iped-engine — `done`
- [ISSUE-098](../issues/ISSUE-098-audit-datasource-specific-types.md) — Audit for datasource-specific types in iped-engine-core — `done`

## Phase 2 — Configuration system completion — `done`
- [ISSUE-099](../issues/ISSUE-099-finish-toml-migration-cleanup.md) — Finish TOML migration cleanup (remove .txt fallback paths) — `done`
- [ISSUE-100](../issues/ISSUE-100-schema-validation-startup.md) — Schema validation at startup with actionable error messages — `done`
- [ISSUE-101](../issues/ISSUE-101-typed-config-api-via-iped-api.md) — Typed config API consumed via iped-api contracts — `done`
- [ISSUE-102](../issues/ISSUE-102-document-config-layering-model.md) — Document the config layering model in module README — `done`

## Phase 3 — Multi-case readiness — `done`
- [ISSUE-103](../issues/ISSUE-103-per-case-context-config-state.md) — Ensure all config/registry state is per-CaseContext — `done`
- [ISSUE-104](../issues/ISSUE-104-concurrent-multicase-stress-tests.md) — Stress-test search/data primitives under concurrent multi-case access — `done`

## Phase 4 — 5.0 — `done`
- [ISSUE-105](../issues/ISSUE-105-stabilize-datasource-spi.md) — Stabilize the datasource SPI for external/closed-source plugins — `done`

## Progress checks
- `mvn -pl iped-engine-parent/iped-engine-core test` green; ArchUnit boundary test present.
- Zero grep hits for sleuthkit/ufed/ad1 imports in `src/main/java`.
