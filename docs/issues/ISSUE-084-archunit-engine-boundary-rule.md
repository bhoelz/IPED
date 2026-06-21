# ISSUE-084: ArchUnit rule forbidding engine imports of app/task-impl/parser-impl types

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 1 — Slim down to orchestration
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add an ArchUnit rule ensuring `iped-engine` cannot import `iped.app.*`, task-impl, or parser-impl types, locking in the orchestration-only boundary.

## Problem

Without an enforced rule, orchestration code in `iped-engine` could silently regain dependencies on UI, task implementations, or parser implementations, eroding the module boundary over time.

## Acceptance criteria

- [x] Add `EngineBoundaryTest` checking no Swing, no Kafka, no `iped.app.*` imports from engine orchestration/config/task/lucene packages.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `done`.
