# ISSUE-103: Ensure all config/registry state is per-CaseContext

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 3 — Multi-case readiness
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Eliminate process-global mutable config/registry state, ensuring everything is scoped per `CaseContext`, per `ARCHITECTURE.md`.

## Problem

A cross-case config corruption bug existed where singleton-scoped state (`findObject`, `getTaskConfigurable`, `getEnableTaskProperty`) leaked across concurrently running cases.

## Acceptance criteria

- [x] `ConfigurationManager.createCaseInstance(IConfigurationDirectory)` factory added.
- [x] `singleton.` references in `findObject`, `getTaskConfigurable`, `getEnableTaskProperty` fixed to `this.`.
- [x] Deprecated `get()`/`createInstance()` singleton retained only for the monolithic launch path.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
