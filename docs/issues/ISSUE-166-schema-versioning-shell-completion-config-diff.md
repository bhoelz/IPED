# ISSUE-166: Configuration schema versioning, shell completion, and config diff/merge/audit

- Status: planned
- Roadmap: [iped-engine-schemas-ROADMAP.md](../roadmaps/iped-engine-schemas-ROADMAP.md)
- Roadmap section: Phase 4 — Advanced Features
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add advanced configuration-management features on top of the schema system: schema versioning with auto-migration, generated shell completion scripts, configuration diff/3-way-merge tooling, and a configuration-change audit log.

## Problem

As schemas evolve across releases, there is currently no migration path for old configs, no shell completion for configuration CLIs, no tooling to compare/merge configuration files, and no audit trail of who changed what configuration and when.

## Acceptance criteria

- [ ] Add a version field to all schemas; create a migration system for schema changes; document breaking changes; auto-migrate old configurations while maintaining backward compatibility.
- [ ] Generate bash and zsh completion scripts; document installation; test with common shells.
- [ ] Create `ConfigurationDiff` and `ConfigurationMerge` classes; implement 3-way merge; generate diff reports; handle merge conflicts with a conflict-resolution UI.
- [ ] Create `ConfigurationAudit` class logging all configuration changes with timestamps and actor; generate audit reports; support configuration rollback.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-schemas-ROADMAP.md` (Phase 4 — Advanced Features, sections 4.1-4.4). Status set to `planned`.
