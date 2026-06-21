# ISSUE-030: Consider JPMS module-info / explicit Automatic-Module-Name

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 3 — Modularization support
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Consider adopting a JPMS `module-info` (or at minimum an explicit `Automatic-Module-Name`) for `iped-api` once consumers stabilize, to support future modularization.

## Problem

Without a stable module name, downstream JPMS adoption (full `module-info` or automatic modules) is harder to coordinate across consumers.

## Acceptance criteria

- [x] `Automatic-Module-Name: iped.api` added via the `maven-jar-plugin` MANIFEST entry in `pom.xml`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
