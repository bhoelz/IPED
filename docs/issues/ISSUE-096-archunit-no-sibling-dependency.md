# ISSUE-096: ArchUnit rule forbidding dependency on iped-engine-parent siblings

- Status: done
- Roadmap: [iped-engine-core-ROADMAP.md](../roadmaps/iped-engine-core-ROADMAP.md)
- Roadmap section: Phase 1 — Boundary enforcement
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add an ArchUnit rule ensuring `iped-engine-core` has no dependency on any `iped-engine-parent` sibling module.

## Problem

As the shared abstraction layer at the bottom of the dependency graph, `iped-engine-core` must never import Sleuthkit, UFED, or Lucene types from sibling modules. Without an enforced rule, this boundary could erode silently.

## Acceptance criteria

- [x] Add `EngineCoreBoundaryTest` checking no Sleuthkit, UFED, or Lucene imports.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-core-ROADMAP.md`, status set to `done`.
