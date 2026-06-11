# IPED configuration

Since the TOML migration, IPED runs **without any local configuration**: every
module ships safe defaults as classpath resources (`iped/config/defaults/`
inside each jar, discovered via `META-INF/iped/config-defaults.idx`).

Local files are **deviation-only**: they may contain just the keys you want to
change. Layers are merged key by key, last one wins:

1. built-in module defaults (inside the jars)
2. plugin jar configs (`pluginFolder`)
3. `IPEDConfig.toml`, `LocalConfig.toml` and `conf/` in the application folder
4. selected profile (`profiles/<name>/`)
5. case profile (`profile/` folder, if present)

## Formats

- `*.toml` — all key/value and list configurations (TOML 1.0), including the
  task pipeline (`TaskInstaller.toml`).
- `*.xml` — parser/carver wiring (`ParserConfig.xml`, `CarverConfig.xml`,
  `CustomSignatures.xml`, ...), unchanged.
- `*.json` — structured configs (`CategoriesConfig.json`, `GraphConfig.json`,
  ...), unchanged.

## Profiles

A profile is still just a folder with configuration files, but each file now
only lists the keys that differ from the defaults — see `profiles/fastmode/`
for an example. List values (e.g. `categories` in `CategoriesToExpand.toml`)
are merged by union across layers.

## Migrating old `.txt` configs

Use the migration tool to convert a pre-TOML configuration tree or profile
into deviation-only TOML files:

```
java -classpath iped.jar iped.app.tools.ConfigMigrationTool <old-config-dir> <output-dir>
```
