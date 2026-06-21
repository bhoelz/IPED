# ISSUE-022: Add ArchUnit test forbidding iped-api dependencies on other modules

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 1 — Contract hygiene
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Add an ArchUnit test that forbids `iped-api` from depending on any other IPED module, enforcing the module's role as the dependency-free public contract layer.

## Problem

Without an automated check, `iped-api` could accidentally accrue a dependency on `iped.engine`, `iped.app`, `iped.parsers`, or similar implementation modules, breaking its role as the lowest-level contract layer.

## Acceptance criteria

- [x] `IpedApiArchitectureTest` added.
- [x] Test checks for forbidden imports of `iped.engine`, `iped.app`, `iped.parsers`, etc.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
