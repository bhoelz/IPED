# ISSUE-163: Runtime schema validation integration and test suite

- Status: planned
- Roadmap: [iped-engine-schemas-ROADMAP.md](../roadmaps/iped-engine-schemas-ROADMAP.md)
- Roadmap section: Phase 1 — Integration & Testing
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Integrate the generated JSON schemas into the live configuration-loading path (`Configuration.getInstance()`), and build the unit/integration test suite proving the schemas correctly validate real configuration files.

## Problem

The schemas and utility classes exist, but nothing in the actual configuration-loading code path consults them yet, and there is no test suite proving the schemas are correct or that they catch real validation errors without producing false positives on existing configs.

## Acceptance criteria

- [ ] Add schema validation to `Configuration.getInstance()`.
- [ ] Create a `ConfigurationValidator` class.
- [ ] Log validation errors without failing on warnings; add validation metrics/reporting.
- [ ] Create validation middleware for `Configuration`.
- [ ] `SchemaValidator` unit tests.
- [ ] `ConfigurableSchemaGenerator` unit tests.
- [ ] Test schema validation against real configs; test schema validity against the JSON Schema meta-schema.
- [ ] CLI schema validation tests.
- [ ] Load real configuration files from `iped-app/resources/config/conf/` and validate against generated schemas with zero validation errors on existing configs.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-schemas-ROADMAP.md` (Phase 1 — Integration & Testing, sections 1.1-1.3), grouping the runtime-validation wiring with the unit/integration test goals for that same phase. Status set to `planned`.
