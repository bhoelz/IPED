# ISSUE-100: Schema validation at startup with actionable error messages

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 2 — Configuration system completion
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Fail fast at startup with actionable error messages (unknown key, wrong type, missing required) before processing begins.

## Problem

Configuration errors discovered mid-processing are costly to diagnose; validating eagerly at startup surfaces problems immediately with clear, actionable messages.

## Acceptance criteria

- [x] `ConfigurationManager.loadConfigs()` calls `validateAllOrFail()` after loading.
- [x] Throws `IllegalStateException` with component names on any schema violation.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
