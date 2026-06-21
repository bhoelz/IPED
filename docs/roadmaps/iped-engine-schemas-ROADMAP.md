# iped-engine-schemas — JSON & UI Schema Implementation Roadmap

> Module purpose: JSON Schema + UI Schema generation/validation for IPED's configurable
> components (`iped-engine/src/main/resources/schemas`) — Java utility classes, generated
> schemas, CLI tooling, and the longer-term web configuration UI/IDE integration plan.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Current state — `done`
- [ISSUE-158](../issues/ISSUE-158-schema-java-utility-classes.md) — Java utility classes for schema generation — `done`
- [ISSUE-159](../issues/ISSUE-159-json-schemas-configurable-components.md) — JSON schemas for configurable components (31 files) — `done`
- [ISSUE-160](../issues/ISSUE-160-ui-schemas-configurable-components.md) — UI schemas for configurable components (31 files) — `done`
- [ISSUE-161](../issues/ISSUE-161-cli-application-schemas.md) — CLI application schemas (JSON + UI, 6 files) — `done`
- [ISSUE-162](../issues/ISSUE-162-schema-documentation-registries.md) — Documentation and registries for the schema system — `done`

## Phase 1 — Integration & testing — `planned`
- [ISSUE-163](../issues/ISSUE-163-runtime-schema-validation-integration-testing.md) — Runtime schema validation integration and test suite — `planned`

## Phase 2 — Schema completion & automation — `planned`
- [ISSUE-164](../issues/ISSUE-164-remaining-schemas-and-generation-tooling.md) — Remaining configurable schemas and automated schema generation tooling — `planned`

## Phase 3 — UI & developer tools — `planned`
- [ISSUE-165](../issues/ISSUE-165-web-config-ui-ide-cli-help-tools.md) — Web configuration UI, IDE integration, and CLI help generation — `planned`

## Phase 4 — Advanced features — `planned`
- [ISSUE-166](../issues/ISSUE-166-schema-versioning-shell-completion-config-diff.md) — Configuration schema versioning, shell completion, and config diff/merge/audit — `planned`

## Phase 5 — Documentation & release — `planned`
- [ISSUE-167](../issues/ISSUE-167-schema-docs-and-release.md) — Documentation and release for the schema system — `planned`

## Priority notes
Relative priority across the planned phases (effort/impact, informal, for sequencing only —
not tracked per-issue):
| Phase | Effort | Impact | Priority |
|---|---|---|---|
| Phase 1 — Integration & testing | Low–Medium | High | Immediate |
| Phase 2 — Schema completion & automation | Low–High | High | High |
| Phase 3 — UI & developer tools | High | High/Medium | Medium |
| Phase 4 — Advanced features | High | Medium | Medium/Low |
| Phase 5 — Documentation & release | Medium | Medium | Low (do alongside the phase it documents) |
