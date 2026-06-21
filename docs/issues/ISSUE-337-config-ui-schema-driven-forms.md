# ISSUE-337: Schema-driven config forms generated from engine-core schema

- Status: planned
- Roadmap: [iped-ui-ROADMAP.md](../roadmaps/iped-ui-ROADMAP.md)
- Roadmap section: Phase 1 — If kept (interim hardening)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Generate configuration editor forms directly from the config schema in `iped-engine-core` (`iped.engine.config.schema`) instead of maintaining hand-built forms, so the UI stays in sync with the engine's actual configuration shape.

## Problem

Hand-built forms drift from the real configuration schema over time, leading to UI fields that don't match what the engine actually accepts or expects.

## Acceptance criteria

- [ ] Editors generated from the `iped.engine.config.schema` schema rather than hand-built per field.
- [ ] Hand-built forms replaced or retired in favor of schema-driven generation.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-ui-ROADMAP.md`, status set to `planned` based on the original `[ ]` marker.
