# iped-engine-core

Shared engine abstractions — the configuration registry and schema system, core data
model, datasource SPI, search primitives, and localization. This module contains **no**
datasource-specific code (no Sleuthkit/UFED/AD1 imports) and no orchestration logic
(no Manager/Worker/Statistics references).

---

## Configuration layering model

IPED resolves configuration in three layers, applied in order from lowest to highest
priority. A value in a higher-priority layer **overrides** the same key from a lower one.

```
┌─────────────────────────────────────────────────┐
│ 3. Local deviation   (case output dir / .local) │  ← highest priority
├─────────────────────────────────────────────────┤
│ 2. Profile           (profiles/<name>/*.toml)   │
├─────────────────────────────────────────────────┤
│ 1. Classpath default (src/main/resources/*.toml)│  ← lowest priority
└─────────────────────────────────────────────────┘
```

### Layer 1 — Classpath defaults

Each module owns its default configuration file on the classpath:

```
iped-engine-core/src/main/resources/conf/
    ProcessingConfig.toml
    LocalConfig.toml
    ...
```

These are the baseline values. Modules must not read `.txt` fallbacks; all defaults
are expressed in TOML.

### Layer 2 — Profile

A profile is a directory of TOML files that override selected keys for a deployment
scenario (e.g. `profiles/pct/`, `profiles/ufed/`). Only keys that differ from the
classpath defaults need to appear in the profile file — absent keys inherit the default.

The active profile is selected by the `--profile` command-line argument.

### Layer 3 — Local deviation

Investigators place a `LocalConfig.toml` in the case output directory (or a sibling
`.local/` directory) to override individual keys for that specific case without
modifying the shared profile. Useful for per-investigation settings such as time
zones, language hints, or disabled tasks.

---

## `ConfigurationManager` — per-case usage

`ConfigurationManager` is the registry that loads, validates, and provides typed access
to all `Configurable<?>` instances for one case.

### Multi-case isolation

For **monolithic single-case** processing the legacy `ConfigurationManager.createInstance()`
and `ConfigurationManager.get()` singleton are still supported (deprecated).

For **multi-case** processing each `CaseContext` must hold its own `ConfigurationManager`
instance so that case A's configuration cannot bleed into case B:

```java
// In multi-case startup code (e.g. ProcessingOrchestrator):
IConfigurationDirectory dir = new ConfigurationDirectory(caseOutputDir, profileDir);
ConfigurationManager cm = ConfigurationManager.createCaseInstance(dir);
// Register all Configurable<?> instances for this case:
cm.addObject(new ProcessingConfig());
cm.addObject(new LocalConfig());
// ...
cm.loadConfigs();   // loads + validates; throws IllegalStateException on schema error

CaseContext ctx = new CaseContext.Builder(UUID.randomUUID())
        .withConfigurationManager(cm)
        // ...
        .build();
```

Tasks and parsers receive `ITypedConfigAccess` (implemented by `ConfigurationManager`)
via injection or the `CaseContext`, so they never need to import `ConfigurationManager`
directly:

```java
// In a task:
ProcessingConfig cfg = configAccess.requireConfig(ProcessingConfig.class);
```

### Schema validation

`loadConfigs()` automatically calls `validateAllOrFail()` after loading all configurables.
If any configurable fails its JSON schema the call throws `IllegalStateException` with a
list of the failing component names. Fix the reported `.toml` file and restart.

---

## ArchUnit boundary rule

`EngineCoreBoundaryTest` enforces that this module contains:

- Zero `org.sleuthkit.*` imports
- Zero `iped.parsers.ufed.*` imports  
- Zero Lucene imports in `Manager`-named classes

Run with `mvn -pl iped-engine-parent/iped-engine-core test`.
