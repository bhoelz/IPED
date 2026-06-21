# ISSUE-307: Add ArchUnit guards for iped-tasks.spi and iped-tasks-forensics

- Status: done
- Roadmap: [iped-tasks-ROADMAP.md](../roadmaps/iped-tasks-ROADMAP.md)
- Roadmap section: Phase 1 — Finish config/code ownership moves
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

ArchUnit guards were added to lock in module boundaries: `iped-tasks.spi` may only
import `iped-api`, and `iped-tasks-forensics` task code may not import `iped.app.*`.

## Problem

Without automated enforcement, the SPI module could accumulate unwanted
dependencies, and forensics task code could acquire an unwanted UI-layer
dependency.

## Acceptance criteria

- [x] ArchUnit guard added to `iped-tasks.spi` restricting imports to `iped-api`.
- [x] ArchUnit guard added to `iped-tasks-forensics` forbidding `iped.app.*` imports.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-tasks-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
