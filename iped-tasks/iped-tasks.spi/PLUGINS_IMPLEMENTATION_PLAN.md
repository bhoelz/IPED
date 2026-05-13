# Task Decoupling via SPI Plugins

## Summary
Design and implement a plugin architecture where task implementations are distributed as independent JARs, discovered at runtime, and loaded without compile-time task dependencies in the main engine modules.  
This plan assumes a new module `iped-tasks.spi` will host the plugin contract and the file `PLUGINS_IMPLEMENTATION_PLAN.md`.

## Implementation Changes
1. Create `iped-tasks.spi` as the stable contract module.
2. Define plugin-facing interfaces and metadata:
- `TaskProvider` (task id, factory/create method, dependency metadata, optional configurables).
- `TaskDescriptor` (id, class/factory reference, ordering/dependency hints, enablement key).
- `TaskDependency` model (required/optional, before/after semantics).
3. Move any plugin-safe shared types needed by providers into `iped-tasks.spi` (or thin wrappers) so providers do not depend on concrete task modules.
4. Keep `iped-engine` depending only on `iped-tasks.spi` for task discovery/registration contracts.

5. Introduce runtime provider discovery in engine:
- Add a loader that scans plugin JARs from `pluginFolder`.
- Build isolated plugin classloaders (parent-first for SPI/engine API, child resolution for plugin internals).
- Use `ServiceLoader<TaskProvider>` to discover providers in each JAR.
- Validate provider metadata and collect a unified task registry.

6. Replace class-name-centric task bootstrap with provider registry:
- Keep legacy `TaskInstaller.xml` support during transition.
- Merge discovered provider tasks plus XML tasks into one registry.
- Resolve execution order using declared dependencies and fail with explicit diagnostics on cycles/missing required deps.
- Instantiate tasks through provider factories instead of direct reflection from main project.

7. Plugin packaging contract:
- Each plugin JAR must include `META-INF/services/<TaskProvider FQCN>`.
- Plugin docs/template for minimal skeleton (provider implementation + service file + optional config resources).
- Startup logs must list loaded providers, skipped providers, and dependency resolution results.

8. Migration strategy:
- Phase 1: add SPI + discovery while preserving current behavior.
- Phase 2: migrate selected existing tasks from `iped-tasks/*` to plugin-provider registration.
- Phase 3: deprecate direct XML class registration for external plugins (retain internal compatibility path if needed).

## Public APIs / Interfaces
- New module API in `iped-tasks.spi`:
- `TaskProvider`
- `TaskDescriptor`
- `TaskDependency`
- Optional: `TaskContext`/`TaskFactory` abstraction if constructor signatures vary.
- Engine-side internal API:
- `PluginTaskLoader`
- `TaskRegistry` (resolved descriptors + factory handles)
- `DependencyResolver` for ordered task graph.

## Test Plan
1. Unit tests for SPI contracts:
- Provider descriptor validation (id uniqueness, required fields).
- Dependency graph resolution (simple chain, diamond, cycle detection, optional deps).
2. Engine integration tests:
- Load one plugin JAR with one provider and run end-to-end task instantiation.
- Load multiple plugin JARs with cross-plugin dependencies.
- Missing required plugin dependency must fail fast with actionable message.
- Optional dependency missing must continue with warning.
3. Backward compatibility tests:
- Existing `TaskInstaller.xml`-only setup continues to run.
- Mixed mode (XML + SPI plugins) loads deterministically.
4. Negative-path tests:
- Broken service file, classloading errors, duplicate task ids, and incompatible provider version handling.
5. Packaging tests:
- Verify plugin JAR template produces a discoverable `ServiceLoader` provider.

## Assumptions and Defaults
- `iped-tasks.spi` does not currently exist in the repository and will be created.
- Java `ServiceLoader` is the default plugin discovery mechanism.
- The main project should not add dependencies on concrete task modules after migration.
- Plugin isolation is classloader-based, but SPI and minimal shared APIs remain parent-loaded.
- Legacy `TaskInstaller.xml` remains supported during migration to reduce rollout risk.
