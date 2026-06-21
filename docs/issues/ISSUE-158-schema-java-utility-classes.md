# ISSUE-158: Java utility classes for schema generation

- Status: done
- Roadmap: [iped-engine-schemas-ROADMAP.md](../roadmaps/iped-engine-schemas-ROADMAP.md)
- Roadmap section: Current Status — Completed
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Build the four core Java utility classes underpinning the configurable-component schema system: property metadata representation, schema info aggregation, schema generation, and validation.

## Problem

Generating and validating JSON/UI schemas for IPED's configurable components needed a small set of reusable Java building blocks rather than ad-hoc per-component code.

## Acceptance criteria

- [x] `ConfigurableProperty` implemented.
- [x] `ConfigurableSchemaInfo` implemented.
- [x] `ConfigurableSchemaGenerator` implemented.
- [x] `SchemaValidator` implemented.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-schemas-ROADMAP.md` ("Current Status — Completed"). This is one of several grouped deliverables from a ~93-checkbox historical completed-work log; grouped per the sizing rule rather than filed as individual issues. Status set to `done`.
