# ISSUE-165: Web configuration UI, IDE integration, and CLI help generation

- Status: planned
- Roadmap: [iped-engine-schemas-ROADMAP.md](../roadmaps/iped-engine-schemas-ROADMAP.md)
- Roadmap section: Phase 3 — UI & Developer Tools
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Build a React-based web UI that renders configuration forms from the UI schemas, IDE plugin support (IntelliJ/VS Code) for schema-aware config editing, and a `HelpGenerator` that auto-generates CLI `--help` text from the JSON schemas.

## Problem

The schemas exist but nothing yet consumes them to give developers or operators a better editing experience — no web form generator, no IDE autocompletion/validation, and CLI help text is still hand-maintained instead of derived from the schemas.

## Acceptance criteria

- [ ] React component library using `@rjsf/core` (or equivalent) to render forms from UI schemas; real-time validation feedback; configuration diff/merge UI; configuration templates.
- [ ] API endpoint to serve schemas; configuration import/export UI.
- [ ] IntelliJ IDEA plugin for schema validation; VS Code extension for schema support; auto-completion and real-time validation in editors.
- [ ] `HelpGenerator` class generating `--help` text from JSON schemas; integrated with the CLI argument parser; command-line help updates dynamically as schemas change.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-schemas-ROADMAP.md` (Phase 3 — UI & Developer Tools, sections 3.1-3.3). Status set to `planned`.
