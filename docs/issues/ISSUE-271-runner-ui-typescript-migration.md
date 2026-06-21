# ISSUE-271: TypeScript migration

- Status: planned
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 3 — Hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Migrating the runner UI codebase from JavaScript to TypeScript has been deliberately deferred; current file count and churn don't justify the migration cost yet.

## Problem

The codebase is plain JavaScript with no static type checking. A full TypeScript migration is not currently worth the cost given the module's size, but should remain visible as a future option.

## Acceptance criteria

- [ ] Re-evaluate migration cost/benefit as the codebase grows.
- [ ] If undertaken, migrate incrementally starting with shared modules (`api.js`).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `in_progress` originally per the `[~]` marker, then re-classified as `planned` since the work itself is intentionally deferred rather than actively underway.
