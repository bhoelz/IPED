# State of IPED — 2026-06-21

Generated from [`docs/roadmaps/`](docs/roadmaps/) and [`docs/issues/`](docs/issues/) as part of the planning-docs consolidation. Regenerate after material roadmap/issue changes; the previous version (if any) is backed up to `docs/reports/STATE-OF-IPED-<date>.md` before this file is overwritten.

## Headline numbers

365 tracked issues across 27 module/cross-cutting roadmaps:

| Status | Count | % |
|---|---|---|
| `done` | 244 | 67% |
| `planned` | 106 | 29% |
| `in_progress` | 10 | 3% |
| `blocked` | 5 | 1% |
| `proposed` | 0 | 0% |
| `cancelled` | 0 | 0% |

The high `done` share reflects that several modules (`iped-api`, `iped-distributed`, `iped-engine-core`, `iped-mcp`, `iped-runner`, `iped-webapi`, `iped-webui`, `iped-webui-server`) have already finished the phase of work captured in their current roadmap — their next roadmap phases will need new issues as that work is defined.

## Per-module status

| Module | Issues | Done | In progress | Planned | Blocked | % done | Roadmap |
|---|---|---|---|---|---|---|---|
| iped-ad1 | 6 | 0 | 0 | 6 | 0 | 0% | [link](docs/roadmaps/iped-ad1-ROADMAP.md) |
| iped-additional-index | 8 | 0 | 0 | 8 | 0 | 0% | [link](docs/roadmaps/iped-additional-index-ROADMAP.md) |
| iped-api | 10 | 10 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-api-ROADMAP.md) |
| iped-app | 10 | 4 | 0 | 6 | 0 | 40% | [link](docs/roadmaps/iped-app-ROADMAP.md) |
| iped-carvers | 10 | 9 | 0 | 0 | 1 | 90% | [link](docs/roadmaps/iped-carvers-ROADMAP.md) |
| iped-distributed | 21 | 21 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-distributed-ROADMAP.md) |
| iped-engine | 12 | 7 | 2 | 0 | 3 | 58% | [link](docs/roadmaps/iped-engine-ROADMAP.md) |
| iped-engine-core | 10 | 10 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-engine-core-ROADMAP.md) |
| iped-engine-graph | 7 | 0 | 0 | 7 | 0 | 0% | [link](docs/roadmaps/iped-engine-graph-ROADMAP.md) |
| iped-engine-hashdb | 8 | 0 | 0 | 8 | 0 | 0% | [link](docs/roadmaps/iped-engine-hashdb-ROADMAP.md) |
| iped-engine-parent | 10 | 2 | 1 | 7 | 0 | 20% | [link](docs/roadmaps/iped-engine-parent-ROADMAP.md) |
| iped-engine-plugin-registry | 16 | 2 | 1 | 13 | 0 | 12% | [link](docs/roadmaps/iped-engine-plugin-registry-ROADMAP.md) |
| iped-engine-schemas | 10 | 5 | 0 | 5 | 0 | 50% | [link](docs/roadmaps/iped-engine-schemas-ROADMAP.md) |
| iped-geo | 7 | 5 | 0 | 2 | 0 | 71% | [link](docs/roadmaps/iped-geo-ROADMAP.md) |
| iped-mcp | 22 | 22 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-mcp-ROADMAP.md) |
| iped-parsers | 19 | 11 | 3 | 5 | 0 | 57% | [link](docs/roadmaps/iped-parsers-ROADMAP.md) |
| iped-runner | 23 | 23 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-runner-ROADMAP.md) |
| iped-runner-ui | 31 | 29 | 0 | 2 | 0 | 93% | [link](docs/roadmaps/iped-runner-ui-ROADMAP.md) |
| iped-sleuthkit | 8 | 0 | 0 | 8 | 0 | 0% | [link](docs/roadmaps/iped-sleuthkit-ROADMAP.md) |
| iped-tasks | 21 | 16 | 1 | 3 | 1 | 76% | [link](docs/roadmaps/iped-tasks-ROADMAP.md) |
| iped-ufed | 7 | 0 | 0 | 7 | 0 | 0% | [link](docs/roadmaps/iped-ufed-ROADMAP.md) |
| iped-ui | 6 | 0 | 0 | 6 | 0 | 0% | [link](docs/roadmaps/iped-ui-ROADMAP.md) |
| iped-utils | 9 | 0 | 0 | 9 | 0 | 0% | [link](docs/roadmaps/iped-utils-ROADMAP.md) |
| iped-viewers | 11 | 8 | 0 | 3 | 0 | 72% | [link](docs/roadmaps/iped-viewers-ROADMAP.md) |
| iped-webapi | 22 | 22 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-webapi-ROADMAP.md) |
| iped-webui | 18 | 18 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-webui-ROADMAP.md) |
| iped-webui-server | 18 | 18 | 0 | 0 | 0 | 100% | [link](docs/roadmaps/iped-webui-server-ROADMAP.md) |
| web-ui-implementation | 5 | 2 | 2 | 1 | 0 | 40% | [link](docs/roadmaps/web-ui-implementation-ROADMAP.md) |

`iped-api`/`iped-carvers`/`iped-utils`/`iped-viewers` don't have a dedicated `docs/issues/` prefix in their filenames the way `iped-engine-*` does — cross-check the `Roadmap:` field inside each issue file if a module name looks ambiguous.

## Highlights by workstream (from the master `ROADMAP.md`)

- **Browser-based main UI**: `iped-webui` and `iped-webui-server` both show their current roadmap phases fully `done` (islands architecture, real backend wiring, auth/CSRF, CI gates) — the next roadmap phase for each needs fresh issues once defined. `iped-ui` (config UI) and `iped-viewers` still have open work deciding the config-UI module's fate and classifying/migrating remaining viewers to the web.
- **Companion desktop app**: `iped-app` is 40% done — release assembly and CLI bootstrap work landed; the companion handoff protocol and installer/update channel are still `planned`.
- **MCP server for AI case interaction**: `iped-mcp` and its `iped-webapi` dependency are both 100% done on their current phase — baseline tool surface, audit trail, tenant isolation, and the v2 webapi contract are all complete.
- **Kafka distributed processing**: `iped-distributed` and `iped-runner`/`iped-runner-ui` (dashboard) are all 100% done on current scope. `iped-engine`'s own distributed-integration phase (Phase 3) is 58% done with 3 `blocked` items (deferred pending coordinated cross-module changes — datasource code move, task-config relocation).
- **Additional data stores (graph/vector/time-series)**: `iped-additional-index` and `iped-engine-graph` are both still 0% — entirely `planned`, no work started yet.
- **Scripting APIs / engine modularization**: `iped-engine-parent`'s cross-cutting module-split effort is only 20% done; most of the actual splitting work is `planned` or has migrated into the now-100%-done sibling modules (`iped-engine-core`, `iped-distributed`), suggesting the parent roadmap's own phase descriptions are due for a refresh next.
- **Datasource readers** (`iped-ad1`, `iped-sleuthkit`, `iped-ufed`, `iped-engine-hashdb`): uniformly 0% done — these are stable, low-churn modules where the roadmap only captures forward-looking hardening/distributed-readiness work, not current defects.

## Top risks (carried from master `ROADMAP.md`)

- Viewer parity delays for specialized formats blocking full Swing retirement.
- Event-model drift between the legacy and Kafka-distributed processing paths.
- Data consistency across complementary stores (graph/vector/time-series) once `iped-additional-index` work starts.
- `iped-engine-parent`'s low completion (20%) relative to its `done` siblings suggests the cross-cutting roadmap needs re-scoping rather than more issues at the same granularity.

## Notable `blocked` issues

- `iped-carvers`: distributed carving byte-range work (1 issue) — depends on the Kafka work-unit model maturing in `iped-distributed`.
- `iped-engine`: 3 issues deferred pending coordinated changes across `iped-sleuthkit`/`iped-ufed`/`iped-ad1` and `iped-tasks-*` modules.
- `iped-tasks`: 1 issue (end-of-case finalization stage) — explicitly noted as "investigated, not yet buildable" pending external prerequisites.

## How this report was built

Counts are a mechanical rollup of the `Status:` field in every `docs/issues/ISSUE-*.md` file, grouped by the module each issue's `Roadmap:` link points to. Workstream highlights and risks are a manual read of `docs/roadmaps/*.md` plus the root `ROADMAP.md`. Regenerate by re-running this rollup whenever roadmaps or issue statuses change materially — see `skills/iped-roadmap-tracker/SKILL.md` for the procedure.
