# iped-api — Evolution Roadmap

> Module purpose: the public contract layer of IPED — interfaces for items, case data,
> datasources, search, tasks, configuration, and I/O that all other modules implement or
> consume. No implementation logic, no heavy dependencies.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Stable interface packages: `iped.data`, `iped.datasource`, `iped.search`, `iped.task`,
  `iped.configuration`, `iped.io`, `iped.properties`, `iped.localization`, `iped.exception`.
- Consumed by every other module; the de facto SPI boundary for parsers, tasks, and viewers.
- No formal versioning/deprecation policy yet; API evolves in lockstep with the engine.

## Phase 1 — Contract hygiene
- [ ] Audit all interfaces for engine-implementation leakage (types that force a concrete
      engine class into signatures); replace with API-level abstractions.
- [x] Add ArchUnit test forbidding dependencies from `iped-api` to any other IPED module.
      (`IpedApiArchitectureTest` — checks no `iped.engine`, `iped.app`, `iped.parsers`, etc.)
- [x] Javadoc coverage for every public type (this module is the documentation surface for
      the 5.0 scripting APIs). (`IDataSource`, `IIPEDSource`, `IIPEDSearcher`, `IItemSearcher`,
      `IMultiSearchResult`, `IItemId` all filled in.)
- [x] Mark internal-but-public types with a clear annotation (`@Internal`) so scripting
      docs can exclude them. (`iped.annotation.Internal` added.)

## Phase 2 — Versioned API for IPED 5.0
- [ ] Define a semantic-versioning and deprecation policy (deprecate in 4.x, remove in 5.0).
- [ ] Introduce the stable surface needed by the JS/Python scripting SDKs (workstream 6 of
      the root `ROADMAP.md`): case/query/result navigation, item content/metadata access,
      tagging/bookmarks, export/job orchestration.
- [x] Add API-level event/listener contracts for the distributed pipeline (job lifecycle,
      item processed, case completed) so `iped-distributed` and `iped-runner` don't bind to
      engine internals. (`iped.pipeline.{IJobLifecycleListener, IItemProcessingListener,
      JobLifecycleEvent, ItemProcessingEvent, JobPhase}` added.)

## Phase 3 — Modularization support
- [x] Capability/SPI interfaces for pluggable datasource readers, so `iped-sleuthkit`,
      `iped-ufed`, `iped-ad1` register via `ServiceLoader` instead of hardcoded engine wiring.
      (`iped.datasource.spi.IDataSourceReader` added.)
- [x] Configuration schema contracts aligned with the TOML config system (typed config
      access, validation hooks) without depending on the engine's registry implementation.
      (`iped.configuration.ITypedConfigAccess` added; implemented by `ConfigurationManager`.)
- [ ] Consider JPMS `module-info` (or at least an explicit `Automatic-Module-Name`) once
      consumers stabilize.

## Constraints
- Zero runtime dependencies beyond SLF4J; keep it consumable by external plugin authors.
- Breaking changes require a migration note in `MIGRATION_GUIDE.md`.
