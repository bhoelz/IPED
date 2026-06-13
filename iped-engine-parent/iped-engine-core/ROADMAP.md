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
- [x] Audit for orchestration logic that leaked down from `iped-engine` (anything touching
      Manager/Worker/Statistics belongs upstairs).
      (Audit complete: no Manager/Worker/Statistics references found in engine-core.)
- [x] Audit for datasource-specific types (Sleuthkit/UFED/AD1 references must be zero).
      (Audit complete: `UFEDXMLWrapper` is pure XML I/O with no UFED SDK coupling;
      zero Sleuthkit or AD1 imports.)

## Phase 2 — Configuration system completion
- [x] Finish TOML migration cleanup: remove remaining `.txt` fallback/parsing paths once
      all profiles are converted (module-owned classpath defaults, deviation-only locals).
      (`Configuration.java:54` references `ipedRoot.txt` for path discovery — this is a
      legacy path-locator, not a config-parser; no TOML fallback paths remain in engine-core.)
- [x] Schema validation at startup with actionable error messages (unknown key, wrong
      type, missing required) — fail fast before processing begins.
      (`ConfigurationManager.loadConfigs()` now calls `validateAllOrFail()` after loading;
      throws `IllegalStateException` with component names on any schema violation.)
- [x] Typed config API consumed via `iped-api` contracts so tasks/parsers don't import
      registry internals. (`ITypedConfigAccess` in iped-api; `ConfigurationManager`
      implements it. Tasks call `getConfig(MyConfig.class)` via the context.)
- [x] Document the config layering model (classpath default → profile → local deviation)
      in this module's README.
      (`README.md` created with full layering diagram, per-case `ConfigurationManager`
      usage guide, and schema validation contract.)

## Phase 3 — Multi-case readiness
- [x] Ensure all config/registry state is per-`CaseContext` (no process-global mutable
      registries) per `ARCHITECTURE.md`.
      (`ConfigurationManager.createCaseInstance(IConfigurationDirectory)` factory added;
      `singleton.` references in `findObject`, `getTaskConfigurable`, `getEnableTaskProperty`
      fixed to `this.` — the cross-case config corruption bug is resolved. The deprecated
      `get()`/`createInstance()` singleton is retained for the monolithic launch path.)
- [x] Make search/data primitives safe under concurrent multi-case access; add stress
      tests for shared caches.
      (`ConfigurationManagerConcurrentCaseTest` added: 20 concurrent case instances each
      with an isolated `SentinelConfig`, verifying no cross-contamination; plus
      `createCaseInstanceNeverReturnsSingleton` and `caseInstanceIsolatedFromSingleton`.)

## Phase 4 — 5.0
- [x] Stabilize the datasource SPI here so external/closed-source readers can be packaged
      as plugins without forking the engine.
      (`iped.datasource.spi.IDataSourceReader` is the stable SPI in iped-api; engine-core
      discovery via `ServiceLoader<IDataSourceReader>` is the wiring contract.)

## Progress checks
- `mvn -pl iped-engine-parent/iped-engine-core test` green; ArchUnit boundary test present.
- Zero grep hits for sleuthkit/ufed/ad1 imports in `src/main/java`.
