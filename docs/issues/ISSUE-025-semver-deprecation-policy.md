# ISSUE-025: Define a semantic-versioning and deprecation policy

- Status: done
- Roadmap: [iped-api-ROADMAP.md](../roadmaps/iped-api-ROADMAP.md)
- Roadmap section: Phase 2 — Versioned API for IPED 5.0
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Define a semantic-versioning and deprecation policy for `iped-api` (deprecate in 4.x, remove in 5.0), giving consumers a predictable migration path ahead of the 5.0 release.

## Problem

Without a documented policy, there is no consistent rule for how and when breaking changes to the public API are introduced, making it hard for plugin authors and downstream modules to plan upgrades.

## Acceptance criteria

- [x] `VERSIONING.md` created covering stability tiers, deprecation process, package table, and breaking-change definition.
- [x] `Automatic-Module-Name: iped.api` added to `pom.xml`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-api-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
