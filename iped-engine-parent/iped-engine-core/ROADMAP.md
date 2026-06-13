# iped-engine-core — Evolution Roadmap

> Module purpose: shared engine abstractions — configuration registry/schema, core data
> model, datasource SPI, search primitives, IO, localization. Contains **no**
> datasource-specific or orchestration code.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Hosts the TOML configuration system (`iped.engine.config.{api,registry,schema}`) after
  the migration away from `.txt` configs.
- Sits below the datasource modules and `iped-engine` in the dependency graph.

## Phase 1 — Boundary enforcement
- [x] ArchUnit rule: no dependency on any `iped-engine-parent` sibling.
      (`EngineCoreBoundaryTest` added — checks no Sleuthkit, UFED, or Lucene imports.)
- [ ] Audit for orchestration logic that leaked down from `iped-engine` (anything touching
      Manager/Worker/Statistics belongs upstairs).
- [ ] Audit for datasource-specific types (Sleuthkit/UFED/AD1 references must be zero).

## Phase 2 — Configuration system completion
- [ ] Finish TOML migration cleanup: remove remaining `.txt` fallback/parsing paths once
      all profiles are converted (module-owned classpath defaults, deviation-only locals).
- [x] Schema validation at startup with actionable error messages (unknown key, wrong
      type, missing required) — fail fast before processing begins.
      (`ConfigurationManager.loadConfigs()` now calls `validateAllOrFail()` after loading;
      throws `IllegalStateException` with component names on any schema violation.)
- [x] Typed config API consumed via `iped-api` contracts so tasks/parsers don't import
      registry internals. (`ITypedConfigAccess` in iped-api; `ConfigurationManager`
      implements it. Tasks call `getConfig(MyConfig.class)` via the context.)
- [ ] Document the config layering model (classpath default → profile → local deviation)
      in this module's README.

## Phase 3 — Multi-case readiness
- [ ] Ensure all config/registry state is per-`CaseContext` (no process-global mutable
      registries) per `ARCHITECTURE.md`.
- [ ] Make search/data primitives safe under concurrent multi-case access; add stress
      tests for shared caches.

## Phase 4 — 5.0
- [ ] Stabilize the datasource SPI here so external/closed-source readers can be packaged
      as plugins without forking the engine.

## Progress checks
- `mvn -pl iped-engine-parent/iped-engine-core test` green; ArchUnit boundary test present.
- Zero grep hits for sleuthkit/ufed/ad1 imports in `src/main/java`.
