# ISSUE-102: Document the config layering model in module README

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 2 — Configuration system completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Document the classpath-default → profile → local-deviation config layering model in `iped-engine-core`'s README.

## Problem

The TOML-based config layering model needed a canonical written reference so contributors understand how defaults, profiles, and local deviations interact.

## Acceptance criteria

- [x] `README.md` created with full layering diagram.
- [x] Per-case `ConfigurationManager` usage guide included.
- [x] Schema validation contract documented.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
