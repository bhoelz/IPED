# Component Plugin System — Status (corrected)

This file replaces three documents that previously lived at the repo root —
`IMPLEMENTATION-CHECKLIST.md`, `IMPLEMENTATION-STATUS.md`, and
`PLUGIN-SYSTEM-IMPLEMENTATION-SUMMARY.md` — which were near-duplicate progress
snapshots of the same proposal at different dates, all claiming the work was
substantially or fully (“7/7 Phases Complete (100%)”) done.

## What this proposal is

A generic, cross-component plugin SPI (`ComponentProvider<T>`) intended to let
IPED expose **every** kind of pluggable component — carvers, parsers,
data sources, databases, webhooks, custom metadata, file categories, i18n
bundles — through one discovery/registry/lifecycle mechanism, with carvers
migrated first as the proof of concept. The full design lives in the sibling
`FASE1` through `FASE7` documents in this directory, plus `EXAMPLE-PLUGINS.md`
and `PLUGIN-QUICK-START.md`.

This is **broader than, and unrelated in code to**, the task-only SPI that
this module (`iped-tasks.spi`) actually ships today — `TaskProvider`,
`TaskDescriptor`, `TaskDependency`, `TaskStateModel`, `TaskMetrics` (see
`PLUGINS_IMPLEMENTATION_PLAN.md` and `PLUGIN_TEMPLATE.md` in this same
directory). That task SPI is real, implemented, and tracked as `done` in
[iped-tasks-ROADMAP.md](../../docs/roadmaps/iped-tasks-ROADMAP.md) Phase 2.
The component-plugin-system proposal below was never built on top of it.

## Verified status (2026-06-21), by class name search across the whole repo

| Class / artifact claimed | FASE doc(s) | Found in code? |
|---|---|---|
| `ComponentProvider` | 1 | No |
| `ComponentDescriptor` | 1 | No |
| `ComponentConfigSchema` / `ComponentConfiguration` | 1 | No |
| `ComponentRegistry` | 1 | No |
| `ComponentTaskLoader` | 1 | No |
| `CarverProvider` (generic SPI version) | 2, 3 | No — `iped-carvers-api` has `Carver`/`CarverPlugin`/`CarverConfiguration`, not this |
| `ParserProvider` | 1, 2 | No |
| `DataSourceProvider` | 1, 2 | No |
| `WebhookProvider` | 1, 2 | No |
| `DatabaseProvider` / `SQLiteDatabaseProvider` / `PostgreSQLDatabaseProvider` | 5 | No |
| `EventDispatcher` / `PostProcessingTask` | 5 | No |
| `MetadataSchemaProvider` | 1, 2, 6 | No |
| `MetadataPropertyDescriptor` | 1, 6 | **Yes** — `iped-engine/.../plugins/metadata/MetadataPropertyDescriptor.java` (106 lines) |
| `MetadataRegistry` | 6 | **Yes** — `iped-engine/.../plugins/metadata/MetadataRegistry.java` (185 lines) |
| `FileCategoryProvider` / category registry | 6 | Partial — `iped-engine/.../plugins/categories/FileCategoryRegistry.java` (318 lines) exists, but no `FileCategoryProvider` SPI interface |
| `PluginResourceBundleLoader` (i18n) | 7 | **Yes** — `iped-engine/.../plugins/i18n/PluginResourceBundleLoader.java` (301 lines) |
| FASE 4 (Parser Migration) design doc | referenced by status docs as "100% complete" | **Doc never existed** — no `FASE4-*.md` was ever found in this repo |

## Bottom line

Despite every FASE document and all three removed status files claiming
"✅ COMPLETO" / "100% Complete" for phases 1–7, the actual implementation rate
is close to 0% for the core SPI (`ComponentProvider`/`ComponentRegistry`/
`ComponentTaskLoader` and all the per-type provider interfaces). The only real
code that exists is three standalone classes under
`iped-engine/.../plugins/{metadata,categories,i18n}/` that cover a slice of
what FASE 6 and FASE 7 describe, but built directly against `iped-engine`
rather than through the generic `ComponentProvider` SPI the proposal calls
for — i.e., even those three classes are not wired into the registry/loader
architecture the FASE docs describe.

This proposal should be treated as **not started** for planning purposes, not
as substantially complete. If it's still wanted, it needs to be re-scoped as
fresh roadmap work (see the Phase 5 entry added to
[iped-tasks-ROADMAP.md](../../docs/roadmaps/iped-tasks-ROADMAP.md)), reusing
the existing `MetadataPropertyDescriptor`/`MetadataRegistry`/
`FileCategoryRegistry`/`PluginResourceBundleLoader` classes rather than
restarting from the FASE 6/7 designs as written.
