# ISSUE-447: Evaluate the generic component plugin system proposal

- Status: proposed
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 5 — Component plugin system (proposal)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`iped-tasks.spi/FASE1-COMPONENT-ARCHITECTURE.md` through `FASE7-I18N-POLISH.md`
describe a generic `ComponentProvider<T>` SPI meant to unify discovery and
lifecycle for carvers, parsers, data sources, databases, webhooks, custom
metadata, file categories, and i18n bundles — a superset of, and unrelated in
code to, this module's actual `TaskProvider` SPI.

## Problem

All seven design docs (and three now-removed duplicate status reports) claimed
"100% complete" / "✅ COMPLETO" for every phase. Verification on 2026-06-21
found none of the core classes (`ComponentProvider`, `ComponentRegistry`,
`ComponentTaskLoader`, `CarverProvider`, `ParserProvider`, `DatabaseProvider`,
`EventDispatcher`, `MetadataSchemaProvider`) anywhere in the repository. Only
three narrower, directly-`iped-engine`-coupled classes exist that cover a
slice of FASE 6/7 (`MetadataPropertyDescriptor`, `MetadataRegistry`,
`FileCategoryRegistry`, `PluginResourceBundleLoader`) — none of them wired
into the generic registry/loader architecture the proposal describes. See
[COMPONENT-PLUGIN-SYSTEM-STATUS.md](../../iped-tasks/iped-tasks.spi/COMPONENT-PLUGIN-SYSTEM-STATUS.md)
for the full verification table.

No design decision has been made on whether this generic system is still
wanted now that the narrower `TaskProvider` SPI already ships and covers the
task-extension use case. This issue exists to track that decision, not to
commit to building the original design as written.

## Acceptance criteria

- [ ] Decide whether the generic cross-component SPI is still worth building given `TaskProvider` already covers task plugins.
- [ ] If yes: re-scope FASE 2–7 as fresh, properly-sized issues reusing the existing `MetadataRegistry`/`FileCategoryRegistry`/`PluginResourceBundleLoader` classes rather than restarting from scratch.
- [ ] If no: mark this issue `cancelled` and note the decision rationale here.

## Updates

### 2026-06-21
- Issue created while triaging stray planning docs at the repo `docs/` root. The docs were moved into `iped-tasks/iped-tasks.spi/` and a corrected status doc was written, but no implementation decision has been made — filed as `proposed` pending a go/no-go call.
