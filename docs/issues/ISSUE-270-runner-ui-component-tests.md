# ISSUE-270: Component tests (Vitest + Testing Library)

- Status: planned
- Roadmap: [iped-runner-ui-ROADMAP.md](../roadmaps/iped-runner-ui-ROADMAP.md)
- Roadmap section: Phase 3 — Hardening
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Component-level tests using Vitest and Testing Library have been deliberately deferred; the codebase is currently considered too small to justify the test-harness overhead.

## Problem

There is currently no component test coverage for the runner UI. Adding a full Vitest + Testing Library harness now would add more overhead than value at the project's current size, but the gap should remain tracked.

## Acceptance criteria

- [ ] Re-evaluate test harness need as the component count grows.
- [ ] If adopted, cover at minimum the run detail view, queue panel, and agents panel.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ui-ROADMAP.md`, status set to `in_progress` originally per the `[~]` marker, then re-classified as `planned` since the work itself is intentionally deferred rather than actively underway.
